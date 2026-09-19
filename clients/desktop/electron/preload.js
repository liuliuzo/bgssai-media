'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('bgssaiMediaShell', {
  platform: process.platform,
  shell: 'clients-desktop',
  enterUser: function () {
    return ipcRenderer.invoke('shell:enter', 'user');
  },
  enterAdmin: function () {
    return ipcRenderer.invoke('shell:enter', 'admin');
  },
  showGate: function () {
    return ipcRenderer.invoke('shell:gate');
  },
});
