# BGSSAI Media 六端客户端

依据组织标准（2026-09-19）：每个产品须覆盖 **Windows、macOS、Linux、iOS、Android、微信小程序**。  
先完成代码；服务器与域名后置；本仓不部署。

## 六端定义

| 端 | 形态 | 最低验收 |
| --- | --- | --- |
| Windows | Electron 桌面壳 | 单安装包；闸门双入口；能登录并走核心路径 |
| macOS | 同上 | 同上 |
| Linux | 同上（AppImage 优先） | 同上 |
| iOS | Capacitor | 单 App；闸门双入口；模拟器/真机可跑 |
| Android | 同上 | 同上 |
| 小程序 | 微信原生 | 单 AppID；首页双入口；开发者工具可预览 |

Web 可作为共享 UI，但不能替代六端。小程序不能用公众号 H5 链接替代独立工程。

## 单 App 双入口（全六端）

对齐 Boss 直聘：**每个平台只有一个应用，启动后两个登录入口**。

| 平台组 | 工程 | 规则 |
| --- | --- | --- |
| Win / macOS / Linux | `clients/desktop/` | **一个** Electron；启动闸门「用户登录 / 管理登录」 |
| iOS / Android | `clients/mobile/` | **一个** Capacitor App；同上 |
| 微信小程序 | `clients/miniprogram/` | **一个** AppID；首页同上 |

**禁止**：

- `*-admin` / `*-user` 两套 Electron / APK / IPA
- 两个小程序或「仅公众号 H5」冒充小程序
- 桌面默认直进 user、管理靠隐藏菜单才算双入口（须有显式闸门页）

后端角色仍为 `PLATFORM_ADMIN` / `USER`；壳只做入口分流，不合并鉴权。

## 本仓目录

```text
clients/desktop/     # Electron — 单 App，双入口（Win/macOS/Linux）
clients/mobile/      # Capacitor — 单 App，双入口（iOS+Android）
clients/miniprogram/ # 微信小程序 — 单 AppID，双入口
CLIENTS.md           # 六端状态表
```

## 技术选型

1. **桌面**：Electron；本地 `renderer/gate.html` 闸门；再加载 user/admin Web。
2. **移动**：Capacitor；`www` 闸门；再打开 user（3002）/ admin（3001）或部署 URL。
3. **小程序**：微信原生；首页闸门；用户/管理登录页 + 可选 web-view。

## 与 libVLC 桌面播放器

`bgssai-media-desktop` 继续负责本地全格式播放（对照 VLC）。  
六端桌面壳是 `clients/desktop/`，二者并存、职责不同。

## 诚实状态

见根目录 [CLIENTS.md](../../CLIENTS.md)。打包上架与小程序提审 **未做**。
