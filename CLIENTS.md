# BGSSAI Media 客户端状态

标准：`docs/feature/six-platform-clients.md`（2026-09-19）。2026-10-17 起，系列按端拆成四个仓库。

| 端 | 仓库 | 形态 |
| --- | --- | --- |
| PC 播放器 + 平台 API | 本仓 `bgssai-media-pc` | `bgssai-media-desktop` 参考 VLC，播放主流格式；用户端 / 管理端 API 也在本仓 |
| Web | [bgssai-media-web](https://github.com/liuliuzo/bgssai-media-web) | `user/` 与 `admin/` |
| Android | [bgssai-media-android](https://github.com/liuliuzo/bgssai-media-android) | 原生 Kotlin，ExoPlayer，不用 WebView |
| iOS | [bgssai-media-ios](https://github.com/liuliuzo/bgssai-media-ios) | 原生 SwiftUI，AVPlayer，不用 WebView |

2026-10-06 的播放器/平台源码边界与验收顺序见 [产品定位与交付核查](docs/review/product-direction-20261006.md)。桌面本地播放器与 PC 平台壳尚未联通。Android / iOS 的原生工程已独立成仓，真机播放仍需验收。本仓不再保留 Capacitor 移动壳。

- `web=separate domains OK` — 浏览器用户站 / 管理站可用不同域名或路径；不要求装两套浏览器。
- `installable=single-app dual-entry` — 可安装六端（Win / macOS / Linux / iOS / Android / 微信小程序）每端 **一个安装物**，应用内 **两个登录入口**（用户入口 / 管理入口）。对齐 Boss 直聘。
- `support=in-app messaging` — 站内在线客服（聊优先 + 聊中提工单）：用户站悬浮聊天入口；管理站会话+工单处理台；六端用户/管理入口分别进入同一套能力。详见 `docs/feature/support-messaging.md`。

**禁止** 拆成 desktop-admin + desktop-user、mobile-admin + mobile-user、或两个小程序并行交付。

| 端 | 形态 | 目录 | 约定 | 状态 |
| --- | --- | --- | --- | --- |
| 浏览器 Web | 分域名/路径 | 仓库 `bgssai-media-web` | `web=separate domains OK` | 已迁出 |
| Windows | 单 Electron | `clients/desktop/` | `installable=single-app dual-entry` | 脚手架 |
| macOS | 同上（同一包） | `clients/desktop/` | 同上 | 脚手架 |
| Linux | 同上（AppImage） | `clients/desktop/` | 同上 | 脚手架 |
| iOS | 原生 SwiftUI | 仓库 `bgssai-media-ios` | 在线观看与发布 | 独立仓，待真机验收 |
| Android | 原生 Kotlin | 仓库 `bgssai-media-android` | 在线观看与发布 | 独立仓，待真机验收 |
| 微信小程序 | 单 AppID | `clients/miniprogram/` | 同上 | 脚手架 |

## 与既有模块的关系

| 模块 | 角色 |
| --- | --- |
| `clients/desktop/` | PC 平台壳（Win/macOS/Linux），双登录入口，打开 Web 站 |
| `bgssai-media-desktop/` | 本地全格式播放器（libVLC），不是平台壳 |
| `bgssai-media-android` | Android 播放平台 |
| `bgssai-media-ios` | iOS 播放平台 |
| `clients/miniprogram/` | 微信小程序，双登录入口 |
| `bgssai-media-web` | 用户站与管理站 |

## 本阶段不做

- 商店上架、签名、真机商店包、小程序提审
- 服务器与域名准备、部署流水线
- 任何 admin / user 分装

## 本地预览

```bash
# Web 在 bgssai-media-web
cd clients/desktop && npm install && npm start
# 微信开发者工具打开 clients/miniprogram/
```
