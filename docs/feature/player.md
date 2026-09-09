# 功能：播放器

## Web 短剧端

- 组件：`bgssai-media-user/frontend` UniversalPlayer（video.js + hls.js）
- 支持：MP4 / WebM / HLS
- 能力：URL、本地 File（object URL，限浏览器可解格式）、播放列表、倍速、全屏
- 非浏览器格式：提示改用桌面播放器

## 桌面端（完整矩阵）

- 模块：`bgssai-media-desktop`（Electron 壳 + 系统 libVLC/cvlc）
- 支持矩阵：见 `docs/feature/format-matrix.md`
- UX：打开文件、播放列表、播放/暂停/停止、上一首/下一首、音量、倍速、seek
- 上游参考：https://github.com/videolan/vlc（只读，不 vendor）
