# bgssai-media-desktop

Electron 壳 + 系统 **libVLC / VLC**（`cvlc` RC）桌面播放器，覆盖完整格式矩阵。

短剧 Web 端仍用浏览器播放 MP4/WebM/HLS；本模块负责本地全格式文件。

## 依赖

```bash
# Debian/Ubuntu
sudo apt install -y vlc libvlc-dev
```

不 vendor https://github.com/videolan/vlc 源码树。见 `reference/vlc/`。

## 运行

```bash
cd bgssai-media-desktop
npm install
npm run smoke:vlc   # 无界面：用 cvlc 打开 fixtures
npm start           # Electron UI（画面在 VLC 窗口）
```

## 能力

- 打开本地文件 / 多选播放列表
- 播放、暂停、停止、上一首/下一首、音量、倍速、seek
- 解码引擎：系统 libVLC（与 VLC 相同）

格式矩阵见 `shared/format-matrix.json` 与 `docs/feature/format-matrix.md`。
