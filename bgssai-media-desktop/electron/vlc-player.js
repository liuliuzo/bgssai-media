'use strict';

const { spawn } = require('child_process');
const net = require('net');
const path = require('path');

/**
 * Controls system VLC (libVLC) via the RC (remote control) interface.
 * Video/audio decode is performed by libVLC — covering the full owner format matrix.
 * Electron provides the shell UI; VLC owns the playback window.
 */
class VlcPlayer {
  constructor() {
    this.proc = null;
    this.port = 4212;
    this.host = '127.0.0.1';
    this.playlist = [];
    this.index = 0;
    this.ready = false;
  }

  async ensureStarted() {
    if (this.proc && this.ready) return;
    await this.shutdown();
    const args = [
      '--intf', 'rc',
      '--rc-host', `${this.host}:${this.port}`,
      '--no-one-instance',
      '--quiet',
    ];
    this.proc = spawn('cvlc', args, {
      stdio: ['ignore', 'ignore', 'ignore'],
    });
    this.proc.on('exit', () => {
      this.ready = false;
      this.proc = null;
    });
    await this.waitForPort(40, 100);
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
          if (left <= 0) reject(new Error('VLC RC port not ready; is vlc/cvlc installed?'));
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
      engine: 'libVLC/cvlc-rc',
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
      this.proc.kill('SIGTERM');
    } catch (_) {
      // ignore
    }
    this.proc = null;
    this.ready = false;
  }
}

module.exports = { VlcPlayer };
