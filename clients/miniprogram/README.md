# clients/miniprogram — BGSSAI Media 微信小程序（单 AppID）

**一个** 小程序工程、**一个** AppID。首页提供「用户登录 / 管理登录」双入口（Boss 直聘模式）。

**禁止** 拆成两个小程序，也禁止用公众号 H5 链接冒充本工程。

## 打开

用微信开发者工具导入本目录。`project.config.json` 中 `appid` 现为占位 `touristappid`，上线前换成正式 AppID。

## 配置

`config/env.js`：

- `userApiBase` / `adminApiBase`：后端（本地默认 8081 / 8080）
- `userH5Login` / `adminH5Login`：可选 web-view H5

真机/体验版须配置 request / business 合法域名。

## 页面

| 页面 | 作用 |
| --- | --- |
| `pages/index` | 双入口闸门 |
| `pages/login-user` | 用户密码登录骨架 |
| `pages/login-admin` | 管理密码登录骨架 |
| `pages/home-user` / `home-admin` | 登录后占位主页 |
| `pages/webview` | 可选 H5 承载 |

## 本阶段

开发者工具可预览闸门与登录页。商店提审、正式 AppID、域名与真机联调后置。
