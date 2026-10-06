# bgssai-media 产品愿景

当前定位核查：2026-10-06。完整产品规划见 [产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)；当前源码证据、实现边界及多端验收见 [产品定位与交付核查](docs/review/product-direction-20261006.md)。`docs/review/acceptance-20260911.md` 保留为当日验收记录。规划目标不等于验收通过。

## 核心定位

> **bgssai-media 是播放器 + 视频播放平台：播放器参考 VLC，平台对标 YouTube 和 YouTube Shorts；承接 Short 短剧成片与 Long 长剧成片直接发布。交付目标是主流格式播放及 Windows PC、Web 在线、Android、iOS 多端播放。**

## 闭环（MVP）

```
short 短剧成片  ──┐
                 ├→ 摄入 API（idempotency_key UNIQUE）
long  长剧成片  ──┘     → media 入库（PENDING / FAILED / READY）
                       → Shorts 流（/shorts）或正片 / 剧集流
                       → UniversalPlayer / 桌面 VLC RC 播放
```

契约：[Short 摄入](docs/contracts/short-to-media-publish.md)、[Long 摄入](docs/contracts/long-to-media-publish.md)。该图表示目标链路；真实成片跨仓和多端播放须分别验收。

## 产品定位

| 能力 | 说明 |
| --- | --- |
| 通用播放器 | 对照 VLC。当前桌面模块经 RC 控制外部 VLC，按样本验证矩阵；Web 播放 MP4/WebM/HLS 依赖浏览器及编码。移动原生播放与桌面统一入口仍待实现/验收。详见 `docs/feature/format-matrix.md` |
| 视频平台 | 对标 YouTube（正片 / 剧集）与 YouTube Shorts（短视频 / 短剧）：目录、分集播放、继续观看、Shorts 流 `/shorts` |
| 发布接入 | 承接 `bgssai-short` 与 `bgssai-long` 成片摄入（`idempotency_key` UNIQUE upsert，READY 重放不双写），运营端查看摄入日志 |
| 用户登录 | 密码 + 邮箱 OTP + 手机 OTP；境内四家演示；**Chat 第三方登录为 PREP**（不签发会话） |
| 管理端 | 仅 DML 种子密码登录，**不接入 Chat** |

## 与兄弟产品边界与闭环协同

- **bgssai-short**：**AI 短剧制作平台**（参考 `waoowaoo`、`Jellyfish`、`ArcReel`、`LocalMiniDrama`、`openframe`、`ZJT`）。成品短剧通过标准契约（`/bgssai/user/media/ingest/short`）直接发布到本平台 YouTube Shorts 竖屏流。
- **bgssai-long**：**AI 长剧制作平台**（参考 `waoowaoo`、`Jellyfish`、`ArcReel`、`LocalMiniDrama`、`openframe`、`ZJT`）。成品长剧通过标准契约（`/bgssai/user/media/ingest/long`）直接发布到本平台 YouTube 正片 / 剧集流。
- **bgssai-media**：**播放器 + 视频播放平台**（参考 VLC，对标 YouTube 和 YouTube Shorts）。目标覆盖主流格式并承接 short 与 long 成片，交付 Windows PC、Web、Android、iOS；当前差距与通过条件见本次核查。
- **闭环协同**：`bgssai-short` + `bgssai-long` + `bgssai-media` 形成完整的音视频创作、制作、分发、播放闭环，三仓一体化协同迭代。
- **bgssai-chat**：中心账号；本仓用户端预留第三方登录，管理端隔离。

## 非目标（MVP）

DRM、直播、社区、支付、推荐排序、完整 Chat OAuth、生产 OBS/SMS 凭证；不 vendor Videolan 源码树（用系统 libVLC）。

## 里程碑

历史记录见 `docs/milestones.md`；当前交付证据与顺序见 [产品定位与交付核查](docs/review/product-direction-20261006.md)。已有 Short/Long 摄入和 Web 播放路径，真实成片跨仓与四端播放尚需联合验收。
工作在独立任务分支进行，通过 PR 合入 develop；dev/prod 均部署 develop，分别使用 `application-dev.properties` / `application-prod.properties`。
