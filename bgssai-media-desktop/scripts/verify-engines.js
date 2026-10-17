'use strict';

const fs = require('node:fs');
const { spawnSync } = require('node:child_process');

module.exports = async function verifyEngines() {
  const engines = [require('ffmpeg-static'), require('ffprobe-static').path];
  for (const binary of engines) {
    if (!binary || !fs.existsSync(binary)) {
      throw new Error('桌面包缺少 FFmpeg/FFprobe。请执行依赖安装脚本（node node_modules/ffmpeg-static/install.js）后重新打包。');
    }
    const result = spawnSync(binary, ['-version'], { windowsHide: true, encoding: 'utf8', timeout: 10000 });
    if (result.status !== 0) throw new Error('桌面解码器不可执行，请在目标平台重新安装依赖后打包。');
  }
};
