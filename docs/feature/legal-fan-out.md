# 法务外链（官网权威页 fan-out）

状态：**需法务审阅**。

权威页以官网已发布域名 `https://www.bgssai.com` 的 WordPress 路径为准。本仓只做外链，不做法务正文，不编造证照/许可证编号。禁止使用错误 host（如 `https://bgssai.com`、无 `www`）或错误 slug（如 `/legal/terms`、`/legal/privacy`、`/legal/cookies`、`/legal/legal-notice`）。

## 五条权威页（中文路径 + `/en/` 对照）

正文在官网。产品仓中文标签指向中文 URL，English 标签指向 `/en/{slug}/`。

| id | 中文 | English | 中文 URL | English URL |
| --- | --- | --- | --- | --- |
| terms-of-service | 服务条款 | Terms of Service | https://www.bgssai.com/terms-of-service/ | https://www.bgssai.com/en/terms-of-service/ |
| privacy-policy | 隐私政策 | Privacy Policy | https://www.bgssai.com/privacy-policy/ | https://www.bgssai.com/en/privacy-policy/ |
| personal-information-inventory | 个人信息收集清单 | Personal Information Inventory | https://www.bgssai.com/personal-information-inventory/ | https://www.bgssai.com/en/personal-information-inventory/ |
| third-party-sharing | 第三方信息共享清单 | Third-party Sharing List | https://www.bgssai.com/third-party-sharing/ | https://www.bgssai.com/en/third-party-sharing/ |
| app-permissions | 应用权限说明 | App Permissions | https://www.bgssai.com/app-permissions/ | https://www.bgssai.com/en/app-permissions/ |

禁止在本仓页脚或登录页填写 ICP / 网文 / 视听 / 增值电信等编号。证照以官网法务页为准（待法务审阅后公示）。

## USER

- 登录页：提示登录即阅读并同意官网服务条款与隐私政策，并展示 5 条中英外链（各语言各一 URL）。
- 已登录页脚（桌面独立布局 + 壳内内容区底部）：同样 5 条外链。
- 不新增本仓法务正文路由，不复制官网条款。

## ADMIN

- **仅外链（outbound only）**：登录页与后台页脚展示同一组官网链接。
- 不做“登录即同意”用户授权文案（管理员不接入用户授权）。
- 不新增本仓法务正文页。

## 验收（本仓范围）

- USER `/login` 可见 5 条指向上述 `www.bgssai.com` 权威 slug 的中英链接。
- USER 登录后页脚可见同样 5 条。
- ADMIN 登录页与页脚可见同样 5 条，无同意授权句。
- 全仓检索无法务页使用 `/legal/*` 或非 `www.bgssai.com` host。
- UI 与文档标明 **需法务审阅**。

未执行：法务终稿、官网 5 页全部生产可用性（当前 `terms-of-service` / `privacy-policy` 已发布；`personal-information-inventory` / `third-party-sharing` / `app-permissions` 以官网权威 slug 落地，生产页待官网发布）、证照公示、用户真实同意留痕。

## 2026-09-23 收敛

按 bgssai-skeleton `docs/BGSSAI-Standards.md` 第 15 节「法务同意行与页脚统一口径」收敛，上文「五条中英外链 + 需法务审阅」的界面口径作废，只留作沿革：

- 用户端登录卡内只留一行 `登录或注册即表示已阅读并同意《用户协议》与《隐私政策》`（`bgssai-media-user/frontend/src/legal/LegalConsent.tsx`），书名号在链接文字里；Chat 回调页不放。
- 管理端登录页不放同意句、不放任何法务链接，只靠页脚。
- 用户端与管理端页脚都改成 App 级一行、覆盖含 `/login` 在内的全部路由（`legal/LegalFooter.tsx` 挂在各自 `App.tsx` 根节点）：`© 年份 昆山兵贵神速智能科技有限公司 · 用户协议 · 隐私政策`。本仓没有备案号，不出现、不借别的产品的；备案通过后只补号。年份取当年。
- `legal/catalog.ts` 仍是唯一数据来源，只留 `terms-of-service` / `privacy-policy` 两页（地址不变，`/en/` 保留在数据里，中文界面只渲染中文）；「个人信息收集清单」「第三方信息共享清单」「应用权限说明」从产品端拆掉，只在官网法律信息中心（`https://www.bgssai.com/legal/`）与隐私政策正文内互链。
- 「需法务审阅」「法务正文以官网为准」等内部备注不再渲染到界面，也不再通过 `data-legal-review` 带到 DOM；`LEGAL_REVIEW_STATUS` 常量、`bilingualLabel`、两端的 `LegalLinks.tsx` 与 `.legal-strip*` 样式已删。
- 校验：`bgssai-media-user/frontend` 下 `npm run test:legal`（`catalog.test.ts` 已按新口径改断言，顺带核对管理端登录页与两端 App 级挂载）。
