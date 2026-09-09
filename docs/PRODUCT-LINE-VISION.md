# BGSSAI 产品线规划（副本）

权威原文在 `bgssai-skeleton/docs/PRODUCT-LINE-VISION.md`。本文件为媒体仓工作副本。

1. **BGSSAI** 是给一人公司（OPC）创业者的全行业工具集合。
2. **bgssai-website** 是公司官网；无登录，不承载产品操作。
3. **bgssai-chat** 提供 Web 在线对话 AI（境内/境外分仓）。
4. 中心用户账号在 bgssai-chat；各 App 可用 Chat 做第三方登录，也可自有账号。授权只发生在 user 端；admin 不接 Chat。
5. **bgssai-bot** 对标 Grok Bot，可托管操作各产品。
6. **bgssai-tokenhub** 是模型中枢。
7. 面向 OPC 的工具集合基本是 B2C。
8. 境外应用仅：`bgssai-geo-global`、`bgssai-saas-global`、`bgssai-tokenhub-global`、`bgssai-chat-global`。

有用户端的应用须支持账号密码、邮箱验证码、手机验证码登录。管理端不开放注册，仅 DML 种子账号。未登录默认进登录页。

**本仓**：境内 B2C；媒体播放器 + 短剧分发；承接 bgssai-short 成品发布。
