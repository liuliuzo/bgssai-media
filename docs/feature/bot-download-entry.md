# Bot 下载入口体验

2026-09-26。bgssai-media 用户端在登录页顶部提供常驻的 BGSSAI Bot 下载导航，采用正常文档流，移除悬浮推广卡、底部固定链接及「忽略后永久隐藏」行为。链接不依赖登录，继续进入本应用的 /download/bot。保留既有语言、shell 链接与已在 Bot 内时隐藏重复下载的行为。

BGSSAI Bot 对标 Grok Bot，所有产品应用可经用户授权托管给它直接操作；OpenClaw、Hermes Agent、grok-build、MyContext、DeepSeek Harness 是参考来源，不是本次新增能力。Wiki 另有 Build 下载。

验收：桌面和 320px 手机首屏可以发现入口；按钮触达面积至少 44px 高，键盘焦点可见；不遮挡表单、验证码、第三方登录或法律页脚。下载入口、真实安装包发布、应用托管能力分别验收。

原型：../demo-static/web/user/bot-download-entry.html；详设：../architecture/bot-download-entry.md。
