'use strict';

const { spawn } = require('child_process');
const net = require('net');
const path = require('path');
const { locateVlc, headlessArgs, VlcNotFoundError } = require('./vlc-locator');

/**
 * Controls system VLC (libVLC) via the RC (remote control) interface.
 * Video/audio decode is performed by libVLC — covering the full owner format matrix.
 * Electron provides the shell UI; VLC owns the playback window.
 */
class VlcPlayer {
  /**
   * @param {object} [opts]
   * @param {string} [opts.resourcesPath] Electron process.resourcesPath (bundled engine lookup)
   * @param {Function} [opts.spawn]       injection for tests
   * @param {Function} [opts.locate]      injection for tests
   */
  constructor(opts = {}) {
    this.proc = null;
    this.port = 4212;
    this.host = '127.0.0.1';
    this.playlist = [];
    this.index = 0;
    this.ready = false;
    this.engine = null;        // { binary, source } once located
    this.lastError = null;     // last engine-level error (VLC_NOT_FOUND / VLC_EXITED / VLC_RC_TIMEOUT)
    this.resourcesPath = opts.resourcesPath || null;
    this.spawnImpl = opts.spawn || spawn;
    this.locateImpl = opts.locate || locateVlc;
  }

  /**
   * Resolve the decode engine without starting it. Never throws; returns a status object
   * the UI can render (ok + binary, or ok:false + VLC_NOT_FOUND + install steps).
   */
  probeEngine() {
    try {
      const found = this.locateImpl({ resourcesPath: this.resourcesPath });
      this.engine = { binary: found.binary, source: found.source };
      this.lastError = null;
      return { ok: true, code: 'VLC_FOUND', binary: found.binary, source: found.source, searched: found.searched };
    } catch (err) {
      if (err instanceof VlcNotFoundError) {
        this.engine = null;
        this.lastError = err.toJSON();
        return this.lastError;
      }
      throw err;
    }
  }

  async ensureStarted() {
    if (this.proc && this.ready) return;
    await this.shutdown();
    const probe = this.probeEngine();
    if (!probe.ok) {
      const err = new VlcNotFoundError(probe.platform, probe.searched);
      throw err;
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
      // spawn failure (ENOENT / EACCES) must not crash the main process
      this.ready = false;
      this.proc = null;
      this.lastError = { ok: false, code: 'VLC_SPAWN_FAILED', message: `无法启动 VLC：${err.message}` };
    });
    this.proc.on('exit', (code) => {
      this.ready = false;
      this.proc = null;
      if (code !== 0 && code !== null) {
        this.lastError = { ok: false, code: 'VLC_EXITED', message: `VLC 进程退出（exit=${code}）` };
      }
    });
    try {
      await this.waitForPort(40, 100);
    } catch (err) {
      const e = new Error(`VLC 已找到（${this.engine.binary}）但 RC 控制端口 ${this.host}:${this.port} 未就绪：${err.message}`);
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
    this.playlist = paths.map((p) => path.resolve(p));
    this.index = 0;
    await this.ensureStarted();
    await this.send('clear');
    for (const p of this.playlist) {
      // RC: add <uri>
      const uri = p.includes('://') ? p : `file://${p}`;
      await this.send(`add ${uri}`);
    }
    await this.send('play');
  }

  async play() {
    await this.ensureStarted();
    await this.send('play');
  }

  async pause() {
    await this.send('pause');
  }

  async stop() {
    await this.send('stop');
  }

  async next() {
    await this.send('next');
    if (this.index < this.playlist.length - 1) this.index += 1;
  }

  async prev() {
    await this.send('prev');
    if (this.index > 0) this.index -= 1;
  }

  async seek(seconds) {
    const sec = Math.max(0, Number(seconds) || 0);
    await this.send(`seek ${sec}`);
  }

  async setVolume(volume) {
    // VLC RC volume 0-512; map 0-100 UI to 0-400
    const v = Math.round((Number(volume) || 0) * 4);
    await this.send(`volume ${v}`);
  }

  async setRate(rate) {
    const r = Number(rate) || 1;
    await this.send(`rate ${r}`);
  }

  getStatus() {
    return {
      playlist: this.playlist,
      index: this.index,
      current: this.playlist[this.index] || null,
      ready: this.ready,
      engine: this.engine ? `libVLC/rc (${this.engine.source}: ${this.engine.binary})` : 'libVLC/rc (未定位)',
      engineBinary: this.engine ? this.engine.binary : null,
      lastError: this.lastError,
    };
  }

  async shutdown() {
    if (!this.proc) return;
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

module.exports = { VlcPlayer };
