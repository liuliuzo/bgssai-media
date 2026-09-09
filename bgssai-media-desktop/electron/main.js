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

app.whenReady().then(() => {
  player = new VlcPlayer();
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', async () => {
  if (player) await player.shutdown();
  if (process.platform !== 'darwin') app.quit();
});

ipcMain.handle('player:openFiles', async () => {
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
});

ipcMain.handle('player:openPath', async (_evt, filePath) => {
  await player.openPlaylist([filePath]);
  return { ok: true };
});

ipcMain.handle('player:play', async () => {
  await player.play();
  return { ok: true };
});

ipcMain.handle('player:pause', async () => {
  await player.pause();
  return { ok: true };
});

ipcMain.handle('player:stop', async () => {
  await player.stop();
  return { ok: true };
});

ipcMain.handle('player:next', async () => {
  await player.next();
  return { ok: true };
});

ipcMain.handle('player:prev', async () => {
  await player.prev();
  return { ok: true };
});

ipcMain.handle('player:seek', async (_evt, seconds) => {
  await player.seek(seconds);
  return { ok: true };
});

ipcMain.handle('player:setVolume', async (_evt, volume) => {
  await player.setVolume(volume);
  return { ok: true };
});

ipcMain.handle('player:setRate', async (_evt, rate) => {
  await player.setRate(rate);
  return { ok: true };
});

ipcMain.handle('player:status', async () => {
  return player.getStatus();
});

ipcMain.handle('player:matrix', async () => {
  return require('../shared/format-matrix.json');
});
