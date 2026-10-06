# 格式支持矩阵

本矩阵是样本验收范围，不是四端已通过的证明。2026-10-06 源码核查见 [产品定位与交付核查](../review/product-direction-20261006.md)：当前桌面经 RC 控制外部 VLC；Web 与移动 WebView 受浏览器/系统解码能力限制，Android/iOS 原生本地播放待实现与真机验收。

## 两条播放路径

| 路径 | 模块 | 引擎 | 用途 |
| --- | --- | --- | --- |
| Web 短剧端 | `bgssai-media-user` | video.js + hls.js | 在线短剧目录中的 MP4 / WebM / HLS |
| 桌面播放器 | `bgssai-media-desktop` | 外部系统 VLC（RC 控制） | 本地格式样本验证；VLC 承担播放窗口 |
| Windows 产品壳 | `clients/desktop` | Electron Web 壳 | 在线入口与文件关联声明；尚未接入上述播放器 |
| Android/iOS | `clients/mobile` | Capacitor Web 壳 | 在线入口脚手架；不能据此声明原生全格式播放 |

权威 JSON：`bgssai-media-desktop/shared/format-matrix.json`。

## 桌面端（必须覆盖）

视频容器：`.mp4` `.mkv` `.webm` `.mov` `.avi` `.flv` `.ts` / `.m3u8` `.mpg` `.wmv` `.3gp`

音频：`.mp3` `.m4a` / `.aac` `.ogg` / `.opus` `.flac` `.wav` `.ape` / `.alac` `.wma`

视频编码：H.264/AVC、H.265/HEVC、AV1、VP9、MPEG-2

音频编码：AAC、MP3、Opus、FLAC、PCM

## Web 端（短剧分发）

仅渐进式浏览器友好格式：MP4（H.264/AAC）、WebM、HLS（m3u8）。

## 验证

```bash
cd bgssai-media-desktop && npm run smoke:vlc
```

fixtures 由 ffmpeg 生成，经 cvlc 打开验证 demux/decode。


Fixture inventory and encoder caveats: `bgssai-media-desktop/fixtures/README.md`.

## MEDIA-01 commercial TODO (honest)

These items are **not** closed by ingest unit tests or by marking ingest `READY` under REFERENCE mode.

| # | TODO | State |
| --- | --- | --- |
| 1 | Desktop libVLC smoke across full owner matrix on CI agent with VLC installed | PENDING |
| 2 | Web UniversalPlayer sample set: MP4 / WebM / HLS with owned fixtures (not third-party demos only) | PENDING |
| 3 | Packaged installers (Windows/macOS/Linux) with documented VLC dependency | PENDING |
| 4 | Live bgssai-short publish → media ingest → catalog → UniversalPlayer playback | PENDING |
| 5 | OBS mode: real endpoint/bucket credentials + asset presence check beyond key string | PENDING |
| 6 | Codec edge cases (HEVC/AV1/HDR) signed off against sample library | PENDING |

Do not mark MEDIA-01 commercial-complete until the live Short E2E and format sample gates above are executed and recorded in `docs/review/acceptance-*.md`.
