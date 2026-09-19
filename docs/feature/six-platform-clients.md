# BGSSAI Media 六端客户端

依据组织标准（2026-09-19）：每个产品须覆盖可安装六端 **Windows、macOS、Linux、iOS、Android、微信小程序**（先代码，服务器/域名稍后）。

另有 **浏览器 Web**：用户站与管理站可用不同域名/路径；`web=separate domains OK`。

## 浏览器 vs 可安装端

| 形态 | 交付方式 | Admin / User |
| --- | --- | --- |
| 浏览器 Web | 打开网址即可 | 换域名（或路径）即可；不必装两套浏览器 |
| PC 桌面（Win / macOS / Linux） | 需要安装 | **一个安装包**，应用内两个登录入口 |
| Android / iOS | 需要安装 | **一个 App**，应用内两个登录入口 |
| 微信小程序 | 需要添加小程序 | **一个小程序**，内两个登录入口 |

可安装端标注：`installable=single-app dual-entry`。

## 六端最低验收（可安装）

| 端 | 形态 | 最低验收 |
| --- | --- | --- |
| Windows | 统一 Electron + 双入口 | 两入口均可登录并走通对应核心路径 |
| macOS | 同上 | 同上 |
| Linux | 同上 | 同上 |
| iOS | 统一 App + 双入口 | 同上 |
| Android | 同上 | 同上 |
| 小程序 | 统一小程序 + 双入口 | 开发者工具可预览两入口 |

## 单 App 双入口（全可安装端）

对齐 Boss 直聘：**每个平台只有一个应用，启动后两个登录入口**（用户入口 / 管理入口）。

| 平台组 | 工程 | 规则 |
| --- | --- | --- |
| Win / macOS / Linux | `clients/desktop/` | **一个** Electron；启动闸门「用户入口 / 管理入口」 |
| iOS / Android | `clients/mobile/` | **一个** Capacitor App；同上 |
| 微信小程序 | `clients/miniprogram/` | **一个** AppID；首页同上 |

**禁止**：

- `*-admin` / `*-user` 两套 Electron / APK / IPA / 小程序
- 两个小程序或「仅公众号 H5」冒充小程序
- 桌面默认直进 user、管理靠隐藏菜单才算双入口（须有显式闸门页）

后端角色仍为 `PLATFORM_ADMIN` / `USER`；壳只做入口分流，不合并鉴权。

## 本仓目录

```text
clients/desktop/     # Electron — 单 App，双入口（Win/macOS/Linux）
clients/mobile/      # Capacitor — 单 App，双入口（iOS+Android）
clients/miniprogram/ # 微信小程序 — 单 AppID，双入口
CLIENTS.md           # web=separate domains OK；installable=single-app dual-entry
```

## 技术选型

1. **桌面**：Electron；本地 `renderer/gate.html` 闸门；再加载 user/admin Web。
2. **移动**：Capacitor；`www` 闸门；再打开 user（3002）/ admin（3001）或部署 URL。
3. **小程序**：微信原生；首页闸门；用户/管理登录页 + 可选 web-view。

## 与 libVLC 桌面播放器

`bgssai-media-desktop` 继续负责本地全格式播放（对照 VLC）。  
六端桌面壳是 `clients/desktop/`，二者并存、职责不同。

## 诚实状态

见根目录 [CLIENTS.md](../../CLIENTS.md)。打包上架与小程序提审 **未做**。服务器与域名后置。

站内在线客服：`support=in-app messaging`，见 [support-messaging.md](./support-messaging.md)。
