# BGSSAI Media 六端客户端状态

标准：`docs/feature/six-platform-clients.md`（2026-09-19）。  
Web 用户端（3002）与管理端（3001）是共享 UI；**不能**用仅响应式网页替代六端。

**全六端强制约定（Boss 直聘模式）**：**每一个端都是单个应用 + 两个登录入口**（用户 / 管理）。  
禁止拆成 admin 与 user 两套安装包、两套 Electron、或两个小程序。

| 端 | 形态 | 目录 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| Windows | 单 Electron | `clients/desktop/` | 脚手架 | 闸门双入口 → user / admin |
| macOS | 同上（同一包） | `clients/desktop/` | 脚手架 | 同上 |
| Linux | 同上（AppImage） | `clients/desktop/` | 脚手架 | 同上 |
| iOS | 单 Capacitor App | `clients/mobile/` | 脚手架 | 闸门双入口；与 Android 同工程 |
| Android | 同上（同一包） | `clients/mobile/` | 脚手架 | 无独立 admin APK |
| 微信小程序 | 单 AppID | `clients/miniprogram/` | 脚手架 | 首页双入口 |

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
