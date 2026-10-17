# bgssai-media-desktop

PC 端自己的界面。播放器参考 VLC，同时播放本地文件和平台视频；同一窗口还有平台作品、正片、短视频和创作者中心。用户在创作者中心发布作品，其他用户可从平台作品页选择清晰度观看。

安装了 VLC 时由 VLC 播放；没有 VLC 时由随程序打包的 FFmpeg/FFprobe 实际解码并在桌面窗口输出音画。内置路径先完成本地转换，长视频需等待，准备期间可以停止取消。音轨和文本字幕来自真实媒体流，不支持的输入明确失败。原始文件不改变。

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
npm run smoke:builtin # 实际 Electron 画面与播放控制，不依赖 VLC
npm test             # 含真实 FFmpeg 22 个格式/编码样本
npm start
```

## 能力

- 打开本地文件 / 多选播放列表
- 播放、暂停、停止、上一首/下一首、音量、倍速、seek
- 解码引擎：系统 VLC，或打包的 FFmpeg/FFprobe + 桌面视频窗口

FFmpeg 和 FFprobe 依赖随桌面包分发并解包执行；其许可证和二进制来源说明保留在对应依赖目录。安装依赖时需执行这两项依赖的安装脚本，不能只安装包的元数据。验证范围与 Windows 未签名目录构建限制见 `../docs/test/TC-desktop-real-playback-20261017.md`。

格式矩阵见 `shared/format-matrix.json` 与 `docs/feature/format-matrix.md`。
