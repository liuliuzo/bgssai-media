'use strict';

const { app, BrowserWindow, shell, ipcMain, Menu } = require('electron');
const path = require('path');

/**
 * Admin-site entry only.
 * The PC user UI lives in bgssai-media-desktop and does not load the web user site.
 */
const ADMIN_URL =
  process.env.BGSSAI_MEDIA_ADMIN_URL || 'http://127.0.0.1:3001/login';

let mainWindow = null;
let currentRole = null;

function gatePath() {
  return path.join(__dirname, '..', 'renderer', 'gate.html');
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 960,
    minHeight: 640,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
    title: 'BGSSAI Media',
    show: false,
  });

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  showGate();

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function showGate() {
  currentRole = null;
  if (!mainWindow) {
    return;
  }
  mainWindow.loadFile(gatePath());
  mainWindow.setTitle('BGSSAI Media');
}

function enterRole(role) {
  if (role !== 'admin') {
    return;
  }
  currentRole = 'admin';
  if (!mainWindow) {
    return;
  }
  mainWindow.loadURL(ADMIN_URL);
  mainWindow.setTitle('BGSSAI Media — 管理');
}

function buildMenu() {
  const template = [
    {
      label: '入口',
      submenu: [
        {
          label: '返回双入口',
          click: () => showGate(),
        },
        {
          label: '管理入口',
          click: () => enterRole('admin'),
        },
        { type: 'separator' },
        { role: 'reload' },
        { role: 'quit' },
      ],
    },
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

ipcMain.handle('shell:enter', async (_evt, role) => {
  if (role !== 'admin') {
    return { ok: false, message: 'PC 用户界面在 bgssai-media-desktop，不打开 Web 用户站。' };
  }
  enterRole(role);
  return { ok: true, role: currentRole };
});

ipcMain.handle('shell:gate', async () => {
  showGate();
  return { ok: true };
});

app.whenReady().then(() => {
  buildMenu();
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
