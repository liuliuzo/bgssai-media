# bgssai-media-desktop

PC 端自己的界面。播放器参考 VLC，用系统 **libVLC / VLC**（`cvlc` RC）播放主流格式；同一窗口里还有正片、短视频和创作者中心。用户在创作者中心发布作品。界面不加载 Web 用户站。

在线目录和上传调用本仓用户端，默认 `http://127.0.0.1:8081`。令牌放在请求头 `Jwttoken`。Web 端仍用浏览器播放 MP4/WebM/HLS。

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
