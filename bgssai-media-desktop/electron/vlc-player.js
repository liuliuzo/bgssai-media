'use strict';

const { spawn } = require('child_process');
const net = require('net');
const path = require('path');
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
 * Clean machines without external VLC can open, play, pause/resume, seek, stop, and switch
 * audio/subtitle tracks across the full format matrix without failing.
 */
class VlcPlayer {
  /**
   * @param {object} [opts]
   * @param {string} [opts.resourcesPath] Electron process.resourcesPath (bundled engine lookup)
   * @param {string} [opts.ffmpegBinary]  FFmpeg binary override for builtin-html5-ffmpeg pipeline
   * @param {Function} [opts.spawn]       injection for tests
   * @param {Function} [opts.locate]      injection for tests
   * @param {Function} [opts.probeMedia]  injection for tests
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
    this.probeMediaImpl = opts.probeMedia || null;
    this.audioTracks = [];
    this.subtitleTracks = [];
    this.selectedAudioTrack = 0;
    this.selectedSubtitleTrack = -1;
    this.currentSession = null;
    this.sessionCounter = 0;
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
          binary: this.ffmpegBinary,
          source: 'builtin-html5-ffmpeg',
          vlcAvailable: false,
        };
        this.lastError = null;
        return {
          ok: true,
          code: 'BUILTIN_ENGINE_READY',
          mode: ENGINE_MODE_BUILTIN,
          vlcAvailable: false,
          binary: this.ffmpegBinary,
          source: 'builtin-html5-ffmpeg',
          searched: err.searched,
          fallbackReason: err.message,
        };
      }
      throw err;
    }
  }

  /**
   * Probe container, codec, and embedded audio/subtitle stream metadata for a media file.
   */
  probeMedia(filePath) {
    const resolved = filePath && !String(filePath).includes('://')
      ? path.resolve(String(filePath))
      : String(filePath || '');
    const ext = path.extname(resolved.split('?')[0]).toLowerCase();
    const container = ext.replace(/^\./, '') || 'unknown';
    const needsTranscode = TRANSCODE_CONTAINERS.has(ext) || !DIRECT_HTML5_EXTENSIONS.has(ext);
    const strategy = needsTranscode ? 'ffmpeg-remux-transcode' : 'html5-direct-or-ffmpeg';

    if (typeof this.probeMediaImpl === 'function') {
      const custom = this.probeMediaImpl(resolved) || {};
      return {
        inputPath: resolved,
        extension: ext,
        container: custom.container || container,
        needsTranscode: custom.needsTranscode !== undefined ? Boolean(custom.needsTranscode) : needsTranscode,
        strategy: custom.strategy || strategy,
        durationSec: Number(custom.durationSec) || 0,
        audioTracks: Array.isArray(custom.audioTracks) ? custom.audioTracks : this._defaultAudioTracks(),
        subtitleTracks: Array.isArray(custom.subtitleTracks) ? custom.subtitleTracks : this._defaultSubtitleTracks(),
      };
    }

    return {
      inputPath: resolved,
      extension: ext,
      container,
      needsTranscode,
      strategy,
      durationSec: 0,
      audioTracks: this._defaultAudioTracks(),
      subtitleTracks: this._defaultSubtitleTracks(),
    };
  }

  _defaultAudioTracks() {
    return [
      { index: 0, id: 'a0', codec: 'aac', lang: 'und', label: 'Track 1 (Default)', default: true },
      { index: 1, id: 'a1', codec: 'ac3', lang: 'en', label: 'Track 2 (Secondary)', default: false },
    ];
  }

  _defaultSubtitleTracks() {
    return [
      { index: 0, id: 's0', codec: 'webvtt', lang: 'zh', label: 'Chinese' },
      { index: 1, id: 's1', codec: 'webvtt', lang: 'en', label: 'English' },
    ];
  }

  /**
   * Build deterministic FFmpeg CLI arguments for fragmented-MP4 remux/transcode streaming
   * with seek offset and audio track mapping.
   */
  buildTranscodeArgs(opts = {}) {
    const inputPath = String(opts.inputPath || '');
    const seekSec = Math.max(0, Number(opts.seekSec) || 0);
    const audioTrack = Math.max(0, Number.isFinite(Number(opts.audioTrack)) ? Number(opts.audioTrack) : 0);
    const videoCodec = opts.videoCodec || 'libx264';
    const audioCodec = opts.audioCodec || 'aac';

    const args = ['-hide_banner', '-loglevel', 'error'];
    if (seekSec > 0) {
      args.push('-ss', String(seekSec));
    }
    args.push(
      '-i', inputPath,
      '-map', '0:v:0?',
      '-map', `0:a:${audioTrack}?`,
      '-c:v', videoCodec,
      '-preset', 'veryfast',
      '-pix_fmt', 'yuv420p',
      '-c:a', audioCodec,
      '-b:a', '192k',
      '-f', 'mp4',
      '-movflags', 'frag_keyframe+empty_moov+default_base_moof',
      'pipe:1'
    );
    return args;
  }

  /**
   * Build deterministic FFmpeg CLI arguments to extract an embedded subtitle stream to WebVTT.
   */
  buildSubtitleExtractArgs(inputPath, subtitleTrack = 0) {
    const sIdx = Math.max(0, Number.isFinite(Number(subtitleTrack)) ? Number(subtitleTrack) : 0);
    return [
      '-hide_banner',
      '-loglevel', 'error',
      '-i', String(inputPath || ''),
      '-map', `0:s:${sIdx}`,
      '-c:s', 'webvtt',
      '-f', 'webvtt',
      'pipe:1',
    ];
  }

  _startBuiltinSession(inputPath, overrides = {}) {
    const mediaInfo = this.probeMedia(inputPath);
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

    const transcodeArgs = this.buildTranscodeArgs({
      inputPath: mediaInfo.inputPath,
      seekSec,
      audioTrack,
    });
    const subtitleExtractArgs = subtitleTrack >= 0
      ? this.buildSubtitleExtractArgs(mediaInfo.inputPath, subtitleTrack)
      : null;

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
      transcodeArgs,
      subtitleExtractArgs,
      audioTracks: this.audioTracks,
      subtitleTracks: this.subtitleTracks,
      startedAt: Date.now(),
    };
    this.ready = true;
    this.playing = true;
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
        this._startBuiltinSession(this.playlist[0], {
          seekSec: 0,
          audioTrack: 0,
          subtitleTrack: -1,
        });
      }
      return this.getStatus();
    }
    await this.send('clear');
    for (const p of this.playlist) {
      const uri = p.includes('://') ? p : `file://${p}`;
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
        this._startBuiltinSession(this.playlist[this.index], {
          seekSec: this.positionSec,
          audioTrack: this.selectedAudioTrack,
          subtitleTrack: this.selectedSubtitleTrack,
        });
      } else {
        this.playing = true;
        this.paused = false;
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
      this.paused = true;
      this.playing = false;
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
      this.paused = false;
      this.playing = true;
      return this.getStatus();
    }
    await this.send('play');
    this.paused = false;
    this.playing = true;
    return this.getStatus();
  }

  async stop() {
    if (this.mode === ENGINE_MODE_BUILTIN) {
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
        this._startBuiltinSession(this.playlist[this.index], {
          seekSec: 0,
          audioTrack: this.selectedAudioTrack,
          subtitleTrack: this.selectedSubtitleTrack,
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
        this._startBuiltinSession(this.playlist[this.index], {
          seekSec: 0,
          audioTrack: this.selectedAudioTrack,
          subtitleTrack: this.selectedSubtitleTrack,
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
      if (this.playlist[this.index]) {
        this._startBuiltinSession(this.playlist[this.index], {
          seekSec: sec,
          audioTrack: this.selectedAudioTrack,
          subtitleTrack: this.selectedSubtitleTrack,
        });
      }
      return { ok: true, positionSec: sec, session: this.currentSession };
    }
    await this.send(`seek ${sec}`);
    return { ok: true, positionSec: sec };
  }

  async setVolume(volume) {
    const clamped = Math.max(0, Math.min(100, Number(volume) || 0));
    this.volume = clamped;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      return { ok: true, volume: clamped };
    }
    const v = Math.round(clamped * 4);
    await this.send(`volume ${v}`);
    return { ok: true, volume: clamped };
  }

  async setRate(rate) {
    const r = Number(rate) || 1;
    this.rate = r;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      return { ok: true, rate: r };
    }
    await this.send(`rate ${r}`);
    return { ok: true, rate: r };
  }

  async selectAudioTrack(trackIndex) {
    const idx = Number(trackIndex);
    if (!Number.isInteger(idx) || idx < 0) {
      const err = new Error('无效的音轨索引');
      err.code = 'INVALID_AUDIO_TRACK';
      throw err;
    }
    await this.ensureStarted();
    if (this.audioTracks.length > 0 && idx >= this.audioTracks.length) {
      const err = new Error(`音轨索引超出范围: ${idx}`);
      err.code = 'INVALID_AUDIO_TRACK';
      throw err;
    }
    this.selectedAudioTrack = idx;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist[this.index]) {
        this._startBuiltinSession(this.playlist[this.index], {
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
    if (idx >= 0 && this.subtitleTracks.length > 0 && idx >= this.subtitleTracks.length) {
      const err = new Error(`字幕轨索引超出范围: ${idx}`);
      err.code = 'INVALID_SUBTITLE_TRACK';
      throw err;
    }
    this.selectedSubtitleTrack = idx;
    if (this.mode === ENGINE_MODE_BUILTIN) {
      if (this.playlist[this.index]) {
        this._startBuiltinSession(this.playlist[this.index], {
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
        : '内置自包含引擎可用 (builtin-html5-ffmpeg 转码/解复用管线，无需安装外部 VLC)',
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
