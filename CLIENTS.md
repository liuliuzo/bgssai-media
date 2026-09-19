# BGSSAI Media 六端客户端状态

标准：`docs/feature/six-platform-clients.md`（2026-09-19）。

- `web=separate domains OK` — 浏览器用户站 / 管理站可用不同域名或路径；不要求装两套浏览器。
- `installable=single-app dual-entry` — 可安装六端（Win / macOS / Linux / iOS / Android / 微信小程序）每端 **一个安装物**，应用内 **两个登录入口**（用户入口 / 管理入口）。对齐 Boss 直聘。
- `support=in-app messaging` — 站内在线客服（留言 / 会话）：用户站悬浮入口 + 管理站处理台；六端用户/管理入口分别进入同一套能力。详见 `docs/feature/support-messaging.md`。

**禁止** 拆成 desktop-admin + desktop-user、mobile-admin + mobile-user、或两个小程序并行交付。

| 端 | 形态 | 目录 | 约定 | 状态 |
| --- | --- | --- | --- | --- |
| 浏览器 Web | 分域名/路径 | `bgssai-media-user/frontend`、`bgssai-media-admin/frontend` | `web=separate domains OK` | 已有 |
| Windows | 单 Electron | `clients/desktop/` | `installable=single-app dual-entry` | 脚手架 |
| macOS | 同上（同一包） | `clients/desktop/` | 同上 | 脚手架 |
| Linux | 同上（AppImage） | `clients/desktop/` | 同上 | 脚手架 |
| iOS | 单 Capacitor App | `clients/mobile/` | 同上 | 脚手架 |
| Android | 同上（同一包） | `clients/mobile/` | 同上 | 脚手架 |
| 微信小程序 | 单 AppID | `clients/miniprogram/` | 同上 | 脚手架 |

## 与既有模块的关系

| 模块 | 角色 |
| --- | --- |
| `clients/desktop/` | **唯一** 桌面产品壳（Win/macOS/Linux），双登录入口 |
| `bgssai-media-desktop/` | 本地全格式播放器（libVLC），不是六端产品壳 |
| `clients/mobile/` | **唯一** iOS/Android App，双登录入口 |
| `clients/miniprogram/` | **唯一** 微信小程序，双登录入口 |
| `bgssai-media-user/frontend` | 用户侧 Web UI |
| `bgssai-media-admin/frontend` | 管理侧 Web UI（由同一壳第二入口进入） |

## 本阶段不做

- 商店上架、签名、真机商店包、小程序提审
- 服务器与域名准备、部署流水线
- 任何 admin / user 分装

## 本地预览

```bash
cd bgssai-media-user/frontend && npm install && npm run dev
cd bgssai-media-admin/frontend && npm install && npm run dev

cd clients/desktop && npm install && npm start
cd clients/mobile && npm install && npm run sync
# 微信开发者工具打开 clients/miniprogram/
```
