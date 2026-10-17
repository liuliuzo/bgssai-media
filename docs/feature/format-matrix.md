# 格式支持矩阵

本矩阵是样本验收范围，不是四端已通过的证明。2026-10-17 桌面增加真实 FFmpeg/FFprobe 内置解码与视频输出，VLC 可用时保留原路径；[本轮样本与限制](../test/TC-desktop-real-playback-20261017.md)单独记录。Web 使用浏览器播放器，Android/iOS 使用独立原生工程，三端在线播放受各自解码器限制。

## 验证表（2026-10-12）

机器可读表是 `verified_rows`，三份字节相同：

- `bgssai-media-common/src/main/resources/format/format-matrix.json`（`FormatMatrix`、`GET /bgssai/user/media/formats`）
- `bgssai-media-desktop/shared/format-matrix.json`（`player:matrix` / `player:formatHook`）
- `bgssai-media-web` 的 `user/src/data/format-matrix.json`（`/formats` 与 Web 播放器；与上两份保持一致）

每一行有 `declared` 与 `verification`。当前全部 `verification=NOT_VERIFIED`。没有样本通过记录时不得写成 `VERIFIED_LOCAL`。

`deployment.status=UNDEPLOYED`，`media_user_host` 与 `media_admin_host` 为空。见 [docs/ops/media-hosts.md](../ops/media-hosts.md)。

URL 钩子（Java / 桌面 `format-hook.js` / 前端 `src/format/matrix.ts` 同一组用例）：

| 扩展名 | Web `web_decision` |
| --- | --- |
| `.mp4` `.webm` `.m3u8` | `PLAY` |
| 桌面容器与音频扩展名（`.mkv` 等） | `REJECT` |
| 无扩展名 | `UNKNOWN` |

`REJECT` 只挡住浏览器播放，不把 short/long 摄入判失败。用户端 `/formats` 展示这张表。

## 两条播放路径

| 路径 | 模块 | 引擎 | 用途 |
| --- | --- | --- | --- |
| Web 短剧端 | `bgssai-media-web` | video.js + hls.js | 在线短剧目录中的 MP4 / WebM / HLS |
| 桌面播放器 | `bgssai-media-desktop` | 系统 VLC，或打包的 FFmpeg/FFprobe 与桌面视频窗口 | PC 自己的界面，本地文件与平台视频；内置路径先转换后播放 |
| 管理站入口 | `clients/desktop` | Electron | 只打开管理站，不是用户播放界面 |
| Android / iOS | `bgssai-media-android` / `bgssai-media-ios` | ExoPlayer / AVPlayer | 在线 MP4 / HLS；不能据此声明 VLC 级全格式播放 |

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
