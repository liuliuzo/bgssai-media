# bgssai-media 产品愿景

当前校准版本：2026-09-11。完整产品规划见 [产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)；实现和验证状态见本仓 `docs/review/acceptance-20260911.md`。规划目标不等于验收通过。

## 一句话

**bgssai-media** 是 BGSSAI 的**媒体播放器 + 短剧播放/分发平台**：既能像 VLC 一样打开主流媒体格式本地播放，又承接 **bgssai-short** 已完成短剧的发布与分发（short 明确不做分发/渠道，分发归属本仓）。

## 产品定位

| 能力 | 说明 |
| --- | --- |
| 通用播放器 | **桌面端（libVLC）**覆盖完整容器/编码矩阵；**Web 短剧端**播放在线 MP4/WebM/HLS。详见 `docs/feature/format-matrix.md` |
| 短剧平台 | 剧集目录、分集播放、继续观看（MVP stub）；竖屏短剧与横屏文件两种观看模式 |
| 发布接入 | 提供对 bgssai-short 的服务端发布摄入 API（幂等 upsert），运营端可查看摄入日志 |

## 与兄弟产品边界

- **bgssai-short**：短剧创作/生产；成品发布到本平台，不在 short 内做分发渠道。
- **bgssai-media**：播放与分发；不承担短剧制作流水线。

## 非目标（MVP）

DRM、直播、社区、支付、推荐排序、完整 Chat OAuth、生产 OBS/SMS 凭证；不 vendor Videolan 源码树（用系统 libVLC）。

## 里程碑

见 `docs/milestones.md`。当前交付为 **MVP 可运行垂直切片**。
