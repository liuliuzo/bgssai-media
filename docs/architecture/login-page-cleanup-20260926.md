# 用户登录页整理（详设，2026-09-26）

> 需求：`docs/feature/login-page-cleanup-20260926.md`
> 原型：`docs/demo-static/web/user/login-20260926.html`

| 文件 | 改动 |
| --- | --- |
| `bgssai-media-user/frontend/src/pages/LoginPage.tsx` | 删演示账号 `Alert` 与两条「验证码已发送至…」`Alert`；`Card` 去掉 `title`，卡内加品牌行 `.login-brand` 与 `h1.login-title`；Tab 标签「密码」；`Form` 去掉 `layout="vertical"` 与各 `Form.Item` 的 `label`，输入框改 `aria-label` + 占位符；`countdowns = { email, phone }` 每秒递减，`enterButton` 显示「获取验证码」或「N 秒后重发」，倒计时未归零时 `onSearch` 不发码；验证码主按钮「登录 / 注册」；OAuth 按钮去掉 `title`；`onChat` 未就绪提示固定文案 |
| `bgssai-media-user/frontend/src/components/BotDownloadLayout.css` | 新增 `.login-brand` / `.login-brand-mark` / `.login-title`；第三方按钮网格 `.login-oauth-grid`（`auto-fit, minmax(120px, 1fr)`，品牌名不折行）；`.login-card .phone-dial-field` 加 antd 同款边框、聚焦与报错描边，区号下拉收窄到 88px |
| `bgssai-media-user/frontend/public/brand/bgss-mark.png` | 品牌行用的方形标（与 office 同一张） |

倒计时只在前端防重复点击；真正的限频仍在后端按渠道 + 目标执行。

后端无改动：`AuthService.loginByEmailOtp` / `loginByPhoneOtp` 对未注册的邮箱 / 手机号本来就调 `provisionUser` 自动建号，主按钮「登录 / 注册」与之一致。
