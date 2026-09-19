'use strict';

const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const { VlcPlayer } = require('./vlc-player');

let mainWindow = null;
let player = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 960,
    height: 720,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    title: 'bgssai-media Desktop Player',
  });
  mainWindow.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

/** Wrap an IPC handler so engine errors reach the renderer as data instead of a rejected promise. */
function guarded(fn) {
  return async (...args) => {
    try {
      return await fn(...args);
    } catch (err) {
      if (err && typeof err.toJSON === 'function') return err.toJSON();
      return { ok: false, code: (err && err.code) || 'PLAYER_ERROR', message: err ? err.message : String(err) };
    }
  };
}

app.whenReady().then(() => {
  player = new VlcPlayer({ resourcesPath: app.isPackaged ? process.resourcesPath : null });
  const probe = player.probeEngine();
  if (!probe.ok) {
    // Explicit, visible flow when the decode engine is missing: no silent failure.
    dialog.showMessageBox({
      type: 'warning',
      title: '未找到 VLC 解码引擎',
      message: '本播放器依赖系统 VLC/libVLC 解码。',
      detail: probe.steps.join(String.fromCharCode(10)),
      buttons: ['知道了'],
    });
  }
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', async () => {
  if (player) await player.shutdown();
  if (process.platform !== 'darwin') app.quit();
});

ipcMain.handle('player:openFiles', guarded(async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile', 'multiSelections'],
    filters: [
      {
        name: 'Media',
        extensions: [
          'mp4', 'mkv', 'webm', 'mov', 'avi', 'flv', 'ts', 'm3u8', 'mpg', 'mpeg', 'wmv', '3gp',
          'mp3', 'm4a', 'aac', 'ogg', 'opus', 'flac', 'wav', 'ape', 'alac', 'wma',
        ],
      },
      { name: 'All', extensions: ['*'] },
    ],
  });
  if (result.canceled || result.filePaths.length === 0) {
    return { canceled: true, paths: [] };
  }
  await player.openPlaylist(result.filePaths);
  return { canceled: false, paths: result.filePaths };
}));

ipcMain.handle('player:openPath', guarded(async (_evt, filePath) => {
  await player.openPlaylist([filePath]);
  return { ok: true };
}));

ipcMain.handle('player:play', guarded(async () => {
  await player.play();
  return { ok: true };
}));

ipcMain.handle('player:pause', guarded(async () => {
  await player.pause();
  return { ok: true };
}));

ipcMain.handle('player:stop', guarded(async () => {
  await player.stop();
  return { ok: true };
}));

ipcMain.handle('player:next', guarded(async () => {
  await player.next();
  return { ok: true };
}));

ipcMain.handle('player:prev', guarded(async () => {
  await player.prev();
  return { ok: true };
}));

ipcMain.handle('player:seek', guarded(async (_evt, seconds) => {
  await player.seek(seconds);
  return { ok: true };
}));

ipcMain.handle('player:setVolume', guarded(async (_evt, volume) => {
  await player.setVolume(volume);
  return { ok: true };
}));

ipcMain.handle('player:setRate', guarded(async (_evt, rate) => {
  await player.setRate(rate);
  return { ok: true };
}));

ipcMain.handle('player:status', guarded(async () => {
  return player.getStatus();
}));

ipcMain.handle('player:matrix', guarded(async () => {
  return require('../shared/format-matrix.json');
}));

ipcMain.handle('player:engine', guarded(async () => {
  return player.probeEngine();
}));
