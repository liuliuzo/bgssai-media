'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('mediaDesktop', {
  openFiles: () => ipcRenderer.invoke('player:openFiles'),
  openPath: (p) => ipcRenderer.invoke('player:openPath', p),
  play: () => ipcRenderer.invoke('player:play'),
  pause: () => ipcRenderer.invoke('player:pause'),
  stop: () => ipcRenderer.invoke('player:stop'),
  next: () => ipcRenderer.invoke('player:next'),
  prev: () => ipcRenderer.invoke('player:prev'),
  seek: (s) => ipcRenderer.invoke('player:seek', s),
  setVolume: (v) => ipcRenderer.invoke('player:setVolume', v),
  setRate: (r) => ipcRenderer.invoke('player:setRate', r),
  status: () => ipcRenderer.invoke('player:status'),
  matrix: () => ipcRenderer.invoke('player:matrix'),
});
