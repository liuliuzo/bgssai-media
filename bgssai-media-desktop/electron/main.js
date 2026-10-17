'use strict';

const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const fs = require('fs');
const path = require('path');
const { VlcPlayer } = require('./vlc-player');
const platformApi = require('./platform-api');

let mainWindow = null;
let player = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1180,
    height: 800,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    title: 'BGSSAI Media',
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
  player.probeEngine();
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

ipcMain.handle('player:resume', guarded(async () => {
  await player.resume();
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
  return player.seek(seconds);
}));

ipcMain.handle('player:setVolume', guarded(async (_evt, volume) => {
  return player.setVolume(volume);
}));

ipcMain.handle('player:setRate', guarded(async (_evt, rate) => {
  return player.setRate(rate);
}));

ipcMain.handle('player:selectAudioTrack', guarded(async (_evt, trackIndex) => {
  return player.selectAudioTrack(trackIndex);
}));

ipcMain.handle('player:selectSubtitleTrack', guarded(async (_evt, trackIndex) => {
  return player.selectSubtitleTrack(trackIndex);
}));

ipcMain.handle('player:tracks', guarded(async () => {
  return player.getTracks();
}));

ipcMain.handle('player:status', guarded(async () => {
  return player.getStatus();
}));

ipcMain.handle('player:matrix', guarded(async () => {
  return require('../shared/format-matrix.json');
}));

ipcMain.handle('player:formatHook', guarded(async (_evt, name) => {
  const { hook } = require('../shared/format-hook');
  return hook(require('../shared/format-matrix.json'), name);
}));

ipcMain.handle('player:engine', guarded(async () => {
  return player.probeEngine();
}));

const DEFAULT_BASE = 'http://127.0.0.1:8081';

function sessionFile() {
  return path.join(app.getPath('userData'), 'bgssai-media-session.json');
}

function readSession() {
  try {
    const parsed = JSON.parse(fs.readFileSync(sessionFile(), 'utf8'));
    return {
      base: typeof parsed.base === 'string' && parsed.base ? parsed.base : DEFAULT_BASE,
      token: typeof parsed.token === 'string' ? parsed.token : '',
      username: typeof parsed.username === 'string' ? parsed.username : '',
    };
  } catch (err) {
    return { base: DEFAULT_BASE, token: '', username: '' };
  }
}

function writeSession(next) {
  fs.mkdirSync(path.dirname(sessionFile()), { recursive: true });
  fs.writeFileSync(sessionFile(), JSON.stringify(next));
}

function publicSession(session) {
  return {
    base: session.base,
    username: session.username,
    loggedIn: Boolean(session.token),
  };
}

function clearToken(session) {
  session.token = '';
  session.username = '';
  writeSession(session);
}

ipcMain.handle('platform:session', guarded(async () => {
  return publicSession(readSession());
}));

ipcMain.handle('platform:login', guarded(async (_evt, payload) => {
  const session = readSession();
  const base = platformApi.normalizeBase(payload && payload.base ? payload.base : session.base);
  const username = payload && payload.username ? String(payload.username) : '';
  const password = payload && payload.password ? String(payload.password) : '';
  const result = await platformApi.requestJson(fetch, {
    base,
    method: 'POST',
    apiPath: '/api/auth/login',
    jsonBody: { username, password },
    trustedPath: true,
  });
  const token = result && result.token ? String(result.token) : '';
  if (!token) {
    return { ok: false, code: 'NO_TOKEN', message: '登录没有返回令牌' };
  }
  writeSession({ base, token, username: (result && result.username) || username });
  return publicSession(readSession());
}));

ipcMain.handle('platform:logout', guarded(async () => {
  const session = readSession();
  clearToken(session);
  session.base = session.base || DEFAULT_BASE;
  writeSession(session);
  return publicSession(readSession());
}));

ipcMain.handle('platform:get', guarded(async (_evt, apiPath) => {
  const session = readSession();
  if (!session.token) {
    return { ok: false, code: 'NO_TOKEN', message: '请先登录' };
  }
  try {
    return await platformApi.requestJson(fetch, {
      base: session.base,
      token: session.token,
      apiPath,
    });
  } catch (err) {
    if (err && err.sessionEnded) clearToken(session);
    throw err;
  }
}));

ipcMain.handle('platform:play', guarded(async (_evt, rawUrl) => {
  const session = readSession();
  const target = platformApi.playableHttpUrl(session.base, rawUrl);
  await player.openPlaylist([target]);
  await player.play();
  return { ok: true, url: target };
}));

ipcMain.handle('platform:upload', guarded(async (_evt, payload) => {
  const session = readSession();
  if (!session.token) {
    return { ok: false, code: 'NO_TOKEN', message: '请先登录' };
  }
  const title = payload && payload.title ? String(payload.title).trim() : '';
  const description = payload && payload.description ? String(payload.description) : '';
  if (!title) {
    return { ok: false, code: 'NO_TITLE', message: '请填写标题' };
  }
  const picked = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile'],
    filters: [
      { name: 'Video', extensions: ['mp4', 'mkv', 'webm', 'mov', 'avi', 'm3u8', 'ts', 'flv'] },
      { name: 'All', extensions: ['*'] },
    ],
  });
  if (picked.canceled || picked.filePaths.length === 0) {
    return { canceled: true };
  }
  const filePath = picked.filePaths[0];
  const bytes = await fs.promises.readFile(filePath);
  try {
    const result = await platformApi.uploadVideo(fetch, {
      base: session.base,
      token: session.token,
      title,
      description,
      bytes,
      mime: platformApi.videoMime(filePath),
      filename: path.basename(filePath),
    });
    return { ok: true, result };
  } catch (err) {
    if (err && err.sessionEnded) clearToken(session);
    throw err;
  }
}));
