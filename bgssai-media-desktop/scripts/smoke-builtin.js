'use strict';

// Run with Electron: electron scripts/smoke-builtin.js
// Exercises the actual isolated renderer, preload bridge, decoded frames and controls.
const { app, BrowserWindow, ipcMain } = require('electron');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const { VlcPlayer } = require('../electron/vlc-player');
const { VlcNotFoundError } = require('../electron/vlc-locator');

app.disableHardwareAcceleration();
app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');
const player = new VlcPlayer({ locate: () => { throw new VlcNotFoundError(process.platform, []); } });
for (const name of ['play', 'pause', 'resume', 'stop', 'next', 'prev', 'seek', 'setVolume', 'setRate',
  'selectAudioTrack', 'selectSubtitleTrack']) ipcMain.handle(`player:${name}`, (_evt, arg) => player[name](arg));
ipcMain.handle('player:status', () => player.getStatus());
ipcMain.handle('player:engine', () => player.probeEngine());
ipcMain.handle('player:matrix', () => require('../shared/format-matrix.json'));
ipcMain.handle('player:report', (_evt, report) => player.reportPlayback(report));
ipcMain.handle('platform:session', () => ({ base: 'http://127.0.0.1:8081', loggedIn: false }));
ipcMain.handle('platform:get', () => [{ id: 1, title: '测试创作者作品', description: '本地测试夹具' }]);

async function waitFor(win, expression) {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    if (await win.webContents.executeJavaScript(expression)) return;
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw Error(`Renderer timed out: ${expression}`);
}

app.whenReady().then(async () => {
  const win = new BrowserWindow({ show: false, width: 1180, height: 800,
    webPreferences: { preload: path.resolve(__dirname, '../electron/preload.js'), contextIsolation: true,
      nodeIntegration: false, backgroundThrottling: false, offscreen: true } });
  try {
    await win.loadFile(path.resolve(__dirname, '../renderer/index.html'));
    await player.openPlaylist([path.resolve(__dirname, '../fixtures/sample.mkv')]);
    await win.webContents.executeJavaScript('refreshStatus()');
    await waitFor(win, 'document.getElementById("builtin-video").videoWidth > 0 && document.getElementById("builtin-video").currentTime > 0.2');
    const colors = await win.webContents.executeJavaScript(`(() => {
      const canvas = document.createElement('canvas'); canvas.width = 80; canvas.height = 60;
      const ctx = canvas.getContext('2d'); ctx.drawImage(document.getElementById('builtin-video'), 0, 0, 80, 60);
      const pixels = ctx.getImageData(0, 0, 80, 60).data; const colors = new Set();
      for (let i = 0; i < pixels.length; i += 4) colors.add(pixels[i] + ',' + pixels[i+1] + ',' + pixels[i+2]);
      return colors.size;
    })()`);
    assert.ok(colors > 20, 'decoded frame must contain the real fixture color pattern');
    assert.equal(player.getStatus().playing, true);
    await player.pause();
    await win.webContents.executeJavaScript('refreshStatus()');
    await waitFor(win, 'document.getElementById("builtin-video").paused');
    await player.seek(1.5);
    await win.webContents.executeJavaScript('refreshStatus()');
    await waitFor(win, 'Math.abs(document.getElementById("builtin-video").currentTime - 1.5) < 0.15');
    await player.setVolume(30);
    await player.setRate(1.5);
    await win.webContents.executeJavaScript('refreshStatus()');
    assert.equal(await win.webContents.executeJavaScript('document.getElementById("builtin-video").volume'), 0.3);
    assert.equal(await win.webContents.executeJavaScript('document.getElementById("builtin-video").playbackRate'), 1.5);
    const evidence = path.resolve(__dirname, '../../docs/test/desktop-real-playback-20261017.png');
    fs.writeFileSync(evidence, (await win.webContents.capturePage()).toPNG());
    await player.resume();
    await win.webContents.executeJavaScript('refreshStatus()');
    await waitFor(win, '!document.getElementById("builtin-video").paused');
    await player.stop();
    await win.webContents.executeJavaScript('refreshStatus()');
    assert.equal(await win.webContents.executeJavaScript('document.getElementById("builtin-video").hasAttribute("src")'), false);
    await win.webContents.executeJavaScript('showView("community")');
    await waitFor(win, 'document.getElementById("community-list").textContent.includes("测试创作者作品")');
    console.log('PASS: real decoded frames, pause, resume, seek, volume, rate, stop and creator discovery renderer');
    win.destroy();
    await player.shutdown();
    app.exit(0);
  } catch (err) {
    console.error(err);
    win.destroy();
    await player.shutdown();
    app.exit(1);
  }
});
