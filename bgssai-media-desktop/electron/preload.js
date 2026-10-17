'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('mediaDesktop', {
  openFiles: () => ipcRenderer.invoke('player:openFiles'),
  openPath: (p) => ipcRenderer.invoke('player:openPath', p),
  play: () => ipcRenderer.invoke('player:play'),
  pause: () => ipcRenderer.invoke('player:pause'),
  resume: () => ipcRenderer.invoke('player:resume'),
  stop: () => ipcRenderer.invoke('player:stop'),
  next: () => ipcRenderer.invoke('player:next'),
  prev: () => ipcRenderer.invoke('player:prev'),
  seek: (s) => ipcRenderer.invoke('player:seek', s),
  setVolume: (v) => ipcRenderer.invoke('player:setVolume', v),
  setRate: (r) => ipcRenderer.invoke('player:setRate', r),
  selectAudioTrack: (idx) => ipcRenderer.invoke('player:selectAudioTrack', idx),
  selectSubtitleTrack: (idx) => ipcRenderer.invoke('player:selectSubtitleTrack', idx),
  tracks: () => ipcRenderer.invoke('player:tracks'),
  status: () => ipcRenderer.invoke('player:status'),
  matrix: () => ipcRenderer.invoke('player:matrix'),
  formatHook: (name) => ipcRenderer.invoke('player:formatHook', name),
  engine: () => ipcRenderer.invoke('player:engine'),
  session: () => ipcRenderer.invoke('platform:session'),
  login: (payload) => ipcRenderer.invoke('platform:login', payload),
  logout: () => ipcRenderer.invoke('platform:logout'),
  get: (apiPath) => ipcRenderer.invoke('platform:get', apiPath),
  playUrl: (url) => ipcRenderer.invoke('platform:play', url),
  upload: (payload) => ipcRenderer.invoke('platform:upload', payload),
});
