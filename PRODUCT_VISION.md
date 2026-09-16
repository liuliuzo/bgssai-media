# bgssai-media 产品愿景

当前校准版本：2026-09-16。完整产品规划见 [产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)；实现和验证状态见本仓 `docs/review/acceptance-20260911.md`。规划目标不等于验收通过。

## 一句话

**bgssai-media** 是 BGSSAI 的**播放器 + 视频播放平台**：播放器对照 **VLC**，必须能打开主流媒体格式；平台对标 **YouTube**（正片 / 剧集）与 **YouTube Shorts**（短视频 / 短剧）。`bgssai-short` 做成的短剧、`bgssai-long` 做成的长剧都可以直接发布到本平台播放。short / long 只做制作，分发与播放归属本仓。

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

## 与兄弟产品边界

- **bgssai-short**：短剧**制作**；成品发布到本平台 Shorts 流，不在 short 内做分发渠道。
- **bgssai-long**：长剧**制作**；成品发布到本平台正片 / 剧集流，不在 long 内做播放社区。
- **bgssai-media**：播放器 + 播放平台；不承担 short / long 的制作流水线。
- **bgssai-chat**：中心账号；本仓用户端预留第三方登录，管理端隔离。

## 非目标（MVP）

DRM、直播、社区、支付、推荐排序、完整 Chat OAuth、生产 OBS/SMS 凭证；不 vendor Videolan 源码树（用系统 libVLC）。

## 里程碑

见 `docs/milestones.md`。当前交付为 **MVP 可运行垂直切片 + 发布契约闭环（media 侧）**。
short 仓 PR #54 已合并（用户确认）；本 Agent 仍无法 clone 私仓，见 `docs/contracts/bgssai-short-implement.md`。
