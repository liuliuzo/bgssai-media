# BGSSAI Media 客户端状态

标准：`docs/feature/six-platform-clients.md`（2026-09-19）。2026-10-17 起，系列按端拆成四个仓库。

| 端 | 仓库 | 形态 |
| --- | --- | --- |
| PC 播放器 + 平台 API | 本仓 `bgssai-media-pc` | `bgssai-media-desktop` 有自己的界面：参考 VLC 播主流格式，正片、短视频和创作者中心都在这个应用里 |
| Web | [bgssai-media-web](https://github.com/liuliuzo/bgssai-media-web) | `user/` 与 `admin/`；用户在创作者中心发布作品 |
| Android | [bgssai-media-android](https://github.com/liuliuzo/bgssai-media-android) | 原生 Kotlin，ExoPlayer，不用 WebView；用户在创作者中心发布作品 |
| iOS | [bgssai-media-ios](https://github.com/liuliuzo/bgssai-media-ios) | 原生 SwiftUI，AVPlayer，不用 WebView；用户在创作者中心发布作品 |

2026-10-06 的播放器/平台源码边界与验收顺序见 [产品定位与交付核查](docs/review/product-direction-20261006.md)。PC 用户界面在 `bgssai-media-desktop`，不打开 Web 用户站。Android / iOS 的原生工程已独立成仓，真机播放仍需验收。本仓不再保留 Capacitor 移动壳。

- `web=separate domains OK` — 浏览器用户站 / 管理站可用不同域名或路径；不要求装两套浏览器。
- `installable=single-app dual-entry` — 可安装六端（Win / macOS / Linux / iOS / Android / 微信小程序）每端 **一个安装物**，应用内 **两个登录入口**（用户入口 / 管理入口）。对齐 Boss 直聘。
- `support=in-app messaging` — 站内在线客服（聊优先 + 聊中提工单）：用户站悬浮聊天入口；管理站会话+工单处理台；六端用户/管理入口分别进入同一套能力。详见 `docs/feature/support-messaging.md`。

**禁止** 拆成 desktop-admin + desktop-user、mobile-admin + mobile-user、或两个小程序并行交付。

| 端 | 形态 | 目录 | 约定 | 状态 |
| --- | --- | --- | --- | --- |
| 浏览器 Web | 分域名/路径 | 仓库 `bgssai-media-web` | `web=separate domains OK` | 已迁出 |
| Windows | 单 Electron | `bgssai-media-desktop/` | 自己的播放器、正片、短视频、创作者中心 | 本地界面，待联机验收 |
| macOS | 同上（同一包） | `bgssai-media-desktop/` | 同上 | 本地界面，待联机验收 |
| Linux | 同上（AppImage） | `bgssai-media-desktop/` | 同上 | 本地界面，待联机验收 |
| iOS | 原生 SwiftUI | 仓库 `bgssai-media-ios` | 在线观看；创作者中心发布作品 | 独立仓，待真机验收 |
| Android | 原生 Kotlin | 仓库 `bgssai-media-android` | 在线观看；创作者中心发布作品 | 独立仓，待真机验收 |
| 微信小程序 | 单 AppID | `clients/miniprogram/` | 同上 | 脚手架 |

## 与既有模块的关系

| 模块 | 角色 |
| --- | --- |
| `bgssai-media-desktop/` | PC 用户界面：libVLC 播放器、正片、短视频、创作者中心 |
| `clients/desktop/` | 管理站入口，不承载用户播放界面 |
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
# PC 用户界面
cd bgssai-media-desktop && npm install && npm start
# 管理站入口。先在 bgssai-media-web 启动 admin
cd clients/desktop && npm install && npm start
# 微信开发者工具打开 clients/miniprogram/
```
