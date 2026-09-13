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
