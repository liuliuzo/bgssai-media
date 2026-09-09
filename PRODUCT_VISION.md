# bgssai-media 产品愿景

## 一句话

**bgssai-media** 是 BGSSAI 的**媒体播放器 + 短剧播放/分发平台**：既能像 VLC 一样打开主流媒体格式本地播放，又承接 **bgssai-short** 已完成短剧的发布与分发（short 明确不做分发/渠道，分发归属本仓）。

## 产品定位

| 能力 | 说明 |
| --- | --- |
| 通用播放器 | Web 端优先：MP4（H.264/AAC）、WebM、HLS（m3u8）；打开 URL / 本地文件；播放控制、倍速、全屏、基础播放列表 |
| 短剧平台 | 剧集目录、分集播放、继续观看（MVP stub）；竖屏短剧与横屏文件两种观看模式 |
| 发布接入 | 提供对 bgssai-short 的服务端发布摄入 API（幂等 upsert），运营端可查看摄入日志 |

## 与兄弟产品边界

- **bgssai-short**：短剧创作/生产；成品发布到本平台，不在 short 内做分发渠道。
- **bgssai-media**：播放与分发；不承担短剧制作流水线。

## 非目标（MVP）

原生 VLC/libVLC、桌面安装包、DRM、直播、社区、支付、推荐排序、完整 Chat OAuth、生产 OBS/SMS 凭证。

## 里程碑

见 `docs/milestones.md`。当前交付为 **MVP 可运行垂直切片**。
