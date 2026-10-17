'use strict';

const { spawn } = require('child_process');
const net = require('net');
const path = require('path');
const { pathToFileURL } = require('node:url');
const { BuiltinMedia } = require('./builtin-media');
const { locateVlc, headlessArgs, VlcNotFoundError } = require('./vlc-locator');

const ENGINE_MODE_VLC = 'libvlc-rc';
const ENGINE_MODE_BUILTIN = 'builtin-html5-ffmpeg';

const DIRECT_HTML5_EXTENSIONS = new Set([
  '.mp4', '.webm', '.m3u8', '.mp3', '.wav', '.ogg', '.opus', '.m4a', '.aac', '.flac',
]);

const TRANSCODE_CONTAINERS = new Set([
  '.mkv', '.mov', '.avi', '.flv', '.ts', '.mpg', '.mpeg', '.wmv', '.3gp', '.ape', '.alac', '.wma',
]);

/**
 * Self-contained desktop media player controller.
 *
 * Engine resolution priority:
 *   1. Host or bundled VLC binary via RC interface (mode: 'libvlc-rc', vlcAvailable: true).
 *   2. Built-in self-contained HTML5 + FFmpeg remux/transcode media pipeline
 *      (mode: 'builtin-html5-ffmpeg', vlcAvailable: false) when host VLC is not installed.
 *
 * The fallback prepares real media with FFmpeg and uses actual renderer playback events.
 */
class VlcPlayer {
  /**
   * @param {object} [opts]
   * @param {string} [opts.resourcesPath] Electron process.resourcesPath (bundled engine lookup)
   * @param {string} [opts.ffmpegBinary]  FFmpeg binary override for builtin-html5-ffmpeg pipeline
   * @param {Function} [opts.spawn]       injection for tests
   * @param {Function} [opts.locate]      injection for tests
   * @param {object} [opts.builtin]       decoder injection for lifecycle tests
   */
  constructor(opts = {}) {
    this.proc = null;
    this.transcodeProc = null;
    this.port = opts.port || 4212;
    this.host = opts.host || '127.0.0.1';
    this.playlist = [];
    this.index = 0;
    this.ready = false;
    this.playing = false;
    this.paused = false;
    this.positionSec = 0;
    this.volume = 50;
    this.rate = 1;
    this.engine = null;
    this.mode = null;
    this.vlcAvailable = false;
    this.lastError = null;
    this.resourcesPath = opts.resourcesPath || null;
    this.ffmpegBinary = opts.ffmpegBinary || process.env.BGSSAI_FFMPEG_PATH || 'ffmpeg';
    this.spawnImpl = opts.spawn || spawn;
    this.locateImpl = opts.locate || locateVlc;
    this.audioTracks = [];
    this.subtitleTracks = [];
    this.selectedAudioTrack = 0;
    this.selectedSubtitleTrack = -1;
    this.subtitleDelayMs = 0;
    this.audioDelayMs = 0;
    this.currentSession = null;
    this.sessionCounter = 0;
    this.builtin = opts.builtin || new BuiltinMedia({ ffmpeg: opts.ffmpegBinary, ffprobe: opts.ffprobeBinary });
    this.preparing = false;
    this.desiredPlaying = false;
    this.commandVersion = 0;
    this.seekVersion = 0;
    this.prepareVersion = 0;
  }

  /**
   * Resolve the decode engine without starting it. Never throws; falls back automatically
   * to the built-in self-contained pipeline (`builtin-html5-ffmpeg`) when external VLC is absent.
   */
  probeEngine() {
    try {
      const found = this.locateImpl({ resourcesPath: this.resourcesPath });
      this.vlcAvailable = true;
      this.mode = ENGINE_MODE_VLC;
      this.engine = {
        mode: ENGINE_MODE_VLC,
        binary: found.binary,
        source: found.source,
        vlcAvailable: true,
      };
      this.lastError = null;
      return {
        ok: true,
        code: 'VLC_FOUND',
        mode: ENGINE_MODE_VLC,
        vlcAvailable: true,
        binary: found.binary,
        source: found.source,
        searched: found.searched,
      };
    } catch (err) {
      if (err instanceof VlcNotFoundError) {
        this.vlcAvailable = false;
        this.mode = ENGINE_MODE_BUILTIN;
        this.engine = {
          mode: ENGINE_MODE_BUILTIN,
          binary: this.builtin.ffmpeg || this.ffmpegBinary,
          source: 'builtin-html5-ffmpeg',
          vlcAvailable: false,
        };
        this.lastError = null;
        return {
          ok: true,
          code: 'BUILTIN_ENGINE_READY',
          mode: ENGINE_MODE_BUILTIN,
          vlcAvailable: false,
          binary: this.builtin.ffmpeg || this.ffmpegBinary,
          source: 'builtin-html5-ffmpeg',
          searched: err.searched,
          fallbackReason: err.message,
        };
      }
      throw err;
    }
  }

  async _startBuiltinSession(inputPath, overrides = {}) {
    const version = ++this.prepareVersion;
    this.preparing = true;
    this.ready = false;
    this.playing = false;
    this.currentSession = null;
    this.lastError = null;
    this.audioTracks = [];
    this.subtitleTracks = [];
    let mediaInfo;
    try {
      mediaInfo = await this.builtin.prepare(inputPath, {
        audioTrack: overrides.audioTrack ?? this.selectedAudioTrack,
        subtitleTrack: overrides.subtitleTrack ?? this.selectedSubtitleTrack,
      });
    } catch (err) {
      if (version === this.prepareVersion) {
        this.preparing = false;
        this.lastError = { ok: false, code: err.code || 'MEDIA_DECODE_FAILED', message: err.message };
      }
      throw err;
    }
    if (version !== this.prepareVersion) throw Object.assign(new Error('播放准备已取消'), { code: 'PLAYBACK_CANCELED' });
    this.audioTracks = mediaInfo.audioTracks;
    this.subtitleTracks = mediaInfo.subtitleTracks;

    const seekSec = overrides.seekSec !== undefined
      ? Math.max(0, Number(overrides.seekSec) || 0)
      : this.positionSec;
    const audioTrack = overrides.audioTrack !== undefined
      ? Number(overrides.audioTrack)
      : this.selectedAudioTrack;
    const subtitleTrack = overrides.subtitleTrack !== undefined
      ? Number(overrides.subtitleTrack)
      : this.selectedSubtitleTrack;

    this.positionSec = seekSec;
    this.selectedAudioTrack = audioTrack;
    this.selectedSubtitleTrack = subtitleTrack;

    this.sessionCounter += 1;
    this.currentSession = {
      sessionId: `session-${this.sessionCounter}`,
      mode: ENGINE_MODE_BUILTIN,
      inputPath: mediaInfo.inputPath,
      container: mediaInfo.container,
      extension: mediaInfo.extension,
      strategy: mediaInfo.strategy,
      needsTranscode: mediaInfo.needsTranscode,
      seekSec,
      audioTrack,
      subtitleTrack,
      playbackUrl: mediaInfo.playbackUrl,
      subtitleUrl: mediaInfo.subtitleUrl,
      durationSec: mediaInfo.durationSec,
      audioTracks: this.audioTracks,
      subtitleTracks: this.subtitleTracks,
      startedAt: Date.now(),
    };
    this.ready = true;
    this.preparing = false;
    this.desiredPlaying = true;
    this.commandVersion += 1;
    this.playing = false;
    this.paused = false;
    return this.currentSession;
  }

  async ensureStarted() {
    if (!this.engine) {
      this.probeEngine();
    }
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.ready = true;
      return;
    }
    if (this.proc && this.ready) return;
    await this.shutdown();
    const probe = this.probeEngine();
    if (probe.mode === ENGINE_MODE_BUILTIN) {
      this.ready = true;
      return;
    }
    const args = [
      ...headlessArgs(process.platform),
      '--rc-host', `${this.host}:${this.port}`,
    ];
    this.proc = this.spawnImpl(this.engine.binary, args, {
      stdio: ['ignore', 'ignore', 'ignore'],
      windowsHide: true,
    });
    this.proc.on('error', (err) => {
      this.ready = false;
      this.proc = null;
      this.lastError = { ok: false, code: 'VLC_SPAWN_FAILED', message: `无法启动 VLC: ${err.message}` };
    });
    this.proc.on('exit', (code) => {
      this.ready = false;
      this.proc = null;
      if (code !== 0 && code !== null) {
        this.lastError = { ok: false, code: 'VLC_EXITED', message: `VLC 进程退出 (exit=${code})` };
      }
    });
    try {
      await this.waitForPort(40, 100);
    } catch (err) {
      const e = new Error(`VLC 已找到 (${this.engine.binary}) 但 RC 控制端口 ${this.host}:${this.port} 未就绪: ${err.message}`);
      e.code = 'VLC_RC_TIMEOUT';
      this.lastError = { ok: false, code: e.code, message: e.message };
      await this.shutdown();
      throw e;
    }
    this.ready = true;
    await this.send('volume 200');
  }

  waitForPort(attempts, delayMs) {
    return new Promise((resolve, reject) => {
      let left = attempts;
      const tryOnce = () => {
        const socket = net.connect({ host: this.host, port: this.port }, () => {
          socket.end();
          resolve();
        });
        socket.on('error', () => {
          socket.destroy();
          left -= 1;
          if (left <= 0) reject(new Error('RC 端口连接超时'));
          else setTimeout(tryOnce, delayMs);
        });
      };
      tryOnce();
    });
  }

  send(command) {
    return new Promise((resolve, reject) => {
      const socket = net.connect({ host: this.host, port: this.port }, () => {
        socket.write(`${command}\n`);
        setTimeout(() => {
          socket.end();
          resolve();
        }, 50);
      });
      socket.on('error', reject);
    });
  }

  async openPlaylist(paths) {
    this.playlist = (paths || []).map((p) => (String(p).includes('://') ? String(p) : path.resolve(String(p))));
    this.index = 0;
    this.positionSec = 0;
    this.selectedAudioTrack = 0;
    this.selectedSubtitleTrack = -1;
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist.length > 0) {
        await this._startBuiltinSession(this.playlist[0], {
          seekSec: 0,
          audioTrack: 0,
          subtitleTrack: -1,
        });
      }
      return this.getStatus();
    }
    await this.send('clear');
    for (const p of this.playlist) {
      if (/[\r\n]/.test(p)) throw Object.assign(new Error('媒体路径包含无效换行'), { code: 'INVALID_MEDIA_PATH' });
      const uri = p.includes('://') ? p : pathToFileURL(p).href;
      await this.send(`add ${uri}`);
    }
    await this.send('play');
    this.playing = true;
    this.paused = false;
    return this.getStatus();
  }

  async play() {
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist.length > 0 && !this.currentSession) {
        await this._startBuiltinSession(this.playlist[this.index], {
          seekSec: this.positionSec,
          audioTrack: this.selectedAudioTrack,
          subtitleTrack: this.selectedSubtitleTrack,
        });
      } else {
        this.desiredPlaying = true;
        this.commandVersion += 1;
      }
      return this.getStatus();
    }
    await this.send('play');
    this.playing = true;
    this.paused = false;
    return this.getStatus();
  }

  async pause() {
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.desiredPlaying = false;
      this.commandVersion += 1;
      return this.getStatus();
    }
    await this.send('pause');
    this.paused = true;
    this.playing = false;
    return this.getStatus();
  }

  async resume() {
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.desiredPlaying = true;
      this.commandVersion += 1;
      return this.getStatus();
    }
    await this.send('play');
    this.paused = false;
    this.playing = true;
    return this.getStatus();
  }

  async stop() {
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.prepareVersion += 1;
      await this.builtin.cancel();
      this.preparing = false;
      this.desiredPlaying = false;
      this.currentSession = null;
      this.ready = false;
      await this.builtin.cleanup();
      this.playing = false;
      this.paused = false;
      this.positionSec = 0;
      if (this.transcodeProc) {
        try {
          this.transcodeProc.kill();
        } catch (_) {
          // ignore
        }
        this.transcodeProc = null;
      }
      return this.getStatus();
    }
    await this.send('stop');
    this.playing = false;
    this.paused = false;
    this.positionSec = 0;
    return this.getStatus();
  }

  async next() {
    if (this.index < this.playlist.length - 1) this.index += 1;
    this.positionSec = 0;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist[this.index]) {
        await this._startBuiltinSession(this.playlist[this.index], {
          seekSec: 0,
          audioTrack: 0,
          subtitleTrack: -1,
        });
      }
      return this.getStatus();
    }
    await this.send('next');
    return this.getStatus();
  }

  async prev() {
    if (this.index > 0) this.index -= 1;
    this.positionSec = 0;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist[this.index]) {
        await this._startBuiltinSession(this.playlist[this.index], {
          seekSec: 0,
          audioTrack: 0,
          subtitleTrack: -1,
        });
      }
      return this.getStatus();
    }
    await this.send('prev');
    return this.getStatus();
  }

  async seek(seconds) {
    const sec = Math.max(0, Number(seconds) || 0);
    this.positionSec = sec;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.commandVersion += 1;
      this.seekVersion += 1;
      if (this.currentSession) this.currentSession.seekSec = sec;
      return { ok: true, positionSec: sec, session: this.currentSession };
    }
    await this.send(`seek ${sec}`);
    return { ok: true, positionSec: sec };
  }

  async setVolume(volume) {
    const clamped = Math.max(0, Math.min(100, Number(volume) || 0));
    this.volume = clamped;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.commandVersion += 1;
      return { ok: true, volume: clamped };
    }
    const v = Math.round(clamped * 4);
    await this.send(`volume ${v}`);
    return { ok: true, volume: clamped };
  }

  async setRate(rate) {
    const r = Math.max(0.25, Math.min(4, Number(rate) || 1));
    this.rate = r;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      this.commandVersion += 1;
      return { ok: true, rate: r };
    }
    await this.send(`rate ${r}`);
    return { ok: true, rate: r };
  }

  async fullscreen() {
    if (this.mode === ENGINE_MODE_VLC) await this.send('fullscreen');
    return { ok: true };
  }

  async setSubtitleDelay(ms) {
    const delay = Number(ms) || 0;
    this.subtitleDelayMs = delay;
    if (this.mode === ENGINE_MODE_VLC) {
      // VLC RC uses microseconds
      await this.send(`subdelay ${Math.round(delay * 1000)}`);
    }
    return { ok: true, subtitle_delay_ms: delay };
  }

  async setAudioDelay(ms) {
    const delay = Number(ms) || 0;
    this.audioDelayMs = delay;
    if (this.mode === ENGINE_MODE_VLC) {
      await this.send(`audiodelay ${Math.round(delay * 1000)}`);
    }
    return { ok: true, audio_delay_ms: delay };
  }

  async screenshot() {
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_VLC) {
      await this.send('snapshot');
      return { ok: true, mode: 'vlc' };
    }
    // Builtin: renderer captures the video element; main returns a marker for UI.
    return { ok: true, mode: 'builtin', capture: 'renderer' };
  }

  async openExternalSubtitle(filePath) {
    if (!filePath || typeof filePath !== 'string') {
      const err = new Error('字幕文件路径无效');
      err.code = 'INVALID_SUBTITLE_FILE';
      throw err;
    }
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_VLC) {
      await this.send(`addsubtitle ${filePath}`);
      return { ok: true, path: filePath };
    }
    return { ok: true, path: filePath, mode: 'builtin', note: '外挂字幕由渲染层加载' };
  }

  async selectAudioTrack(trackIndex) {
    const idx = Number(trackIndex);
    if (!Number.isInteger(idx) || idx < 0) {
      const err = new Error('无效的音轨索引');
      err.code = 'INVALID_AUDIO_TRACK';
      throw err;
    }
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_BUILTIN && !this.audioTracks.some((t) => t.index === idx)) {
      const err = new Error(`音轨索引超出范围: ${idx}`);
      err.code = 'INVALID_AUDIO_TRACK';
      throw err;
    }
    this.selectedAudioTrack = idx;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist[this.index]) {
        await this._startBuiltinSession(this.playlist[this.index], {
          seekSec: this.positionSec,
          audioTrack: idx,
          subtitleTrack: this.selectedSubtitleTrack,
        });
      }
      return {
        ok: true,
        selectedAudioTrack: this.selectedAudioTrack,
        audioTracks: this.audioTracks,
        session: this.currentSession,
      };
    }
    await this.send(`atrack ${idx}`);
    return {
      ok: true,
      selectedAudioTrack: this.selectedAudioTrack,
      audioTracks: this.audioTracks,
    };
  }

  async selectSubtitleTrack(trackIndex) {
    const idx = trackIndex === null || trackIndex === undefined || trackIndex === ''
      ? -1
      : Number(trackIndex);
    if (!Number.isInteger(idx) || idx < -1) {
      const err = new Error('无效的字幕轨索引');
      err.code = 'INVALID_SUBTITLE_TRACK';
      throw err;
    }
    await this.ensureStarted();
    if (this.mode === ENGINE_MODE_BUILTIN && idx >= 0 && !this.subtitleTracks.some((t) => t.index === idx)) {
      const err = new Error(`字幕轨索引超出范围: ${idx}`);
      err.code = 'INVALID_SUBTITLE_TRACK';
      throw err;
    }
    this.selectedSubtitleTrack = idx;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist[this.index]) {
        await this._startBuiltinSession(this.playlist[this.index], {
          seekSec: this.positionSec,
          audioTrack: this.selectedAudioTrack,
          subtitleTrack: idx,
        });
      }
      return {
        ok: true,
        selectedSubtitleTrack: this.selectedSubtitleTrack,
        subtitleTracks: this.subtitleTracks,
        session: this.currentSession,
      };
    }
    await this.send(`strack ${idx}`);
    return {
      ok: true,
      selectedSubtitleTrack: this.selectedSubtitleTrack,
      subtitleTracks: this.subtitleTracks,
    };
  }

  getTracks() {
    return {
      audioTracks: this.audioTracks,
      subtitleTracks: this.subtitleTracks,
      selectedAudioTrack: this.selectedAudioTrack,
      selectedSubtitleTrack: this.selectedSubtitleTrack,
    };
  }

  reportPlayback(report) {
    if (!this.currentSession || report.sessionId !== this.currentSession.sessionId) return;
    if (report.commandVersion != null && report.commandVersion < this.commandVersion) return;
    this.playing = report.playing === true;
    this.paused = report.paused === true;
    if (report.readyState >= 2) this.desiredPlaying = this.playing || !this.paused;
    if (Number.isFinite(report.positionSec)) this.positionSec = Math.max(0, report.positionSec);
    if (Number.isFinite(report.volume)) this.volume = Math.max(0, Math.min(100, report.volume));
    if (Number.isFinite(report.rate)) this.rate = Math.max(0.25, Math.min(4, report.rate));
    if (report.error) {
      this.playing = false;
      this.desiredPlaying = false;
      this.lastError = { ok: false, code: 'MEDIA_OUTPUT_FAILED', message: String(report.error) };
    }
    if (report.ended) this.desiredPlaying = false;
  }

  getStatus() {
    if (!this.engine && !this.lastError) {
      try {
        this.probeEngine();
      } catch (err) {
        this.lastError = {
          ok: false,
          code: 'VLC_PROBE_FAILED',
          message: err && err.message ? err.message : String(err),
        };
      }
    }
    const found = Boolean(this.engine);
    const isVlc = Boolean(this.engine && this.engine.vlcAvailable);
    return {
      playlist: this.playlist,
      index: this.index,
      current: this.playlist[this.index] || null,
      ready: this.ready,
      preparing: this.preparing,
      desiredPlaying: this.desiredPlaying,
      commandVersion: this.commandVersion,
      seekVersion: this.seekVersion,
      playing: this.playing,
      paused: this.paused,
      positionSec: this.positionSec,
      volume: this.volume,
      rate: this.rate,
      canDecode: found,
      vlcAvailable: isVlc,
      mode: this.mode,
      browserHardDecode: false,
      decodeStatus: isVlc
        ? 'libVLC 可用'
        : (this.preparing ? '正在准备视频，可停止取消' : '内置 FFmpeg 解码，音画由桌面视频窗口输出'),
      engine: isVlc
        ? `libVLC/rc (${this.engine.source}: ${this.engine.binary})`
        : (found ? `builtin-html5-ffmpeg (${this.engine.source}: ${this.engine.binary})` : null),
      engineBinary: found ? this.engine.binary : null,
      audioTracks: this.audioTracks,
      subtitleTracks: this.subtitleTracks,
      selectedAudioTrack: this.selectedAudioTrack,
      selectedSubtitleTrack: this.selectedSubtitleTrack,
      session: this.currentSession,
      lastError: this.lastError,
    };
  }

  async shutdown() {
    this.prepareVersion += 1;
    await this.builtin.close();
    this.currentSession = null;
    if (this.transcodeProc) {
      try {
        this.transcodeProc.kill();
      } catch (_) {
        // ignore
      }
      this.transcodeProc = null;
    }
    this.playing = false;
    this.paused = false;
    if (!this.proc) {
      this.ready = false;
      return;
    }
    try {
      await this.send('quit');
    } catch (_) {
      // ignore
    }
    try {
      this.proc.kill();
    } catch (_) {
      // ignore
    }
    this.proc = null;
    this.ready = false;
  }
}

module.exports = {
  VlcPlayer,
  ENGINE_MODE_VLC,
  ENGINE_MODE_BUILTIN,
  DIRECT_HTML5_EXTENSIONS,
  TRANSCODE_CONTAINERS,
};
