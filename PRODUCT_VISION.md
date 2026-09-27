# bgssai-media 产品愿景

当前校准版本：2026-09-16。完整产品规划见 [产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)；实现和验证状态见本仓 `docs/review/acceptance-20260911.md`。规划目标不等于验收通过。

## 核心定位

> **bgssai-media 是一个播放器，参考的是 VLC 项目，定位是播放器 + 视频播放平台，对标 YouTube 和 YouTube Shorts，需要能支持主流格式文件的播放；然后它还是一款播放平台，bgssai-short、bgssai-long 做完的短剧可以直接发布到这个平台，支持 Windows PC 桌面软件、Web 在线播放、Android、iOS 多端播放。**

## 闭环（MVP）

```
short 短剧成片  ──┐
                 ├→ 摄入 API（idempotency_key UNIQUE）
long  长剧成片  ──┘     → media 入库（PENDING / FAILED / READY）
                       → Shorts 流（/shorts）或正片 / 剧集流
                       → UniversalPlayer / 桌面 libVLC 播放
```

契约：[docs/contracts/short-to-media-publish.md](docs/contracts/short-to-media-publish.md)。

## 产品定位

| 能力 | 说明 |
| --- | --- |
| 通用播放器 | 对照 VLC。**桌面端（libVLC）**覆盖完整容器/编码矩阵；**Web** 播放在线 MP4/WebM/HLS。详见 `docs/feature/format-matrix.md` |
| 视频平台 | 对标 YouTube（正片 / 剧集）与 YouTube Shorts（短视频 / 短剧）：目录、分集播放、继续观看、Shorts 流 `/shorts` |
| 发布接入 | 承接 `bgssai-short` 与 `bgssai-long` 成片摄入（`idempotency_key` UNIQUE upsert，READY 重放不双写），运营端查看摄入日志 |
| 用户登录 | 密码 + 邮箱 OTP + 手机 OTP；境内四家演示；**Chat 第三方登录为 PREP**（不签发会话） |
| 管理端 | 仅 DML 种子密码登录，**不接入 Chat** |

## 与兄弟产品边界与闭环协同

- **bgssai-short**：**AI 短剧制作平台**（参考 `waoowaoo`、`Jellyfish`、`ArcReel`、`LocalMiniDrama`、`openframe`、`ZJT`）。成品短剧通过标准契约（`/bgssai/user/media/ingest/short`）直接发布到本平台 YouTube Shorts 竖屏流。
- **bgssai-long**：**AI 长剧制作平台**（参考 `waoowaoo`、`Jellyfish`、`ArcReel`、`LocalMiniDrama`、`openframe`、`ZJT`）。成品长剧通过标准契约（`/bgssai/user/media/ingest/long`）直接发布到本平台 YouTube 正片 / 剧集流。
- **bgssai-media**：**播放器 + 视频播放平台**（参考 VLC，对标 YouTube 和 YouTube Shorts）。全面支持主流媒体格式，承接 short 与 long 成片，支持 Windows PC 桌面软件、Web 在线、Android、iOS 多端播放。
- **闭环协同**：`bgssai-short` + `bgssai-long` + `bgssai-media` 形成完整的音视频创作、制作、分发、播放闭环，三仓一体化协同迭代。
- **bgssai-chat**：中心账号；本仓用户端预留第三方登录，管理端隔离。

## 非目标（MVP）

DRM、直播、社区、支付、推荐排序、完整 Chat OAuth、生产 OBS/SMS 凭证；不 vendor Videolan 源码树（用系统 libVLC）。

## 里程碑

见 `docs/milestones.md`。当前交付为 **MVP 可运行垂直切片 + short / long 双端发布摄入闭环**。
三仓（`bgssai-short`、`bgssai-long`、`bgssai-media`）均已本地就绪并统一在 `develop` 工作分支对齐迭代。
