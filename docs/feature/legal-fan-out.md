# 法务外链（官网权威页 fan-out）

状态：**需法务审阅**。

权威清单以官网仓 `bgssai-website` develop 分支 `docs/feature/legal-fan-out-checklist.md` 为准。本仓只做外链，不做法务正文，不编造证照/许可证编号。

本 Agent 无法读取私有官网仓；目录按官网已发布域名 `https://www.bgssai.com` 与清单约定的 5 条 `/legal/*` 权威页落地。旧 WordPress 目前对 `/legal/terms`、`/legal/privacy` 有跳转；其余 3 条以官网新站为准，未审阅前不得写成已合规。

## 五条权威页（双语标签，单一 URL）

正文在官网，页面自身中英对照。产品仓链接文案为 `中文 / English`。

| id | 中文 | English | URL |
| --- | --- | --- | --- |
| terms | 服务条款 | Terms of Service | https://www.bgssai.com/legal/terms |
| privacy | 隐私政策 | Privacy Policy | https://www.bgssai.com/legal/privacy |
| cookies | Cookie 政策 | Cookie Policy | https://www.bgssai.com/legal/cookies |
| third-party-sharing | 第三方信息共享清单 | Third-party Sharing List | https://www.bgssai.com/legal/third-party-sharing |
| legal-notice | 法律声明 | Legal Notice | https://www.bgssai.com/legal/legal-notice |

禁止在本仓页脚或登录页填写 ICP / 网文 / 视听 / 增值电信等编号。证照以官网法务页为准（待法务审阅后公示）。

## USER

- 登录页：提示登录即阅读并同意官网服务条款与隐私政策，并展示 5 条双语外链。
- 已登录页脚（桌面独立布局 + 壳内内容区底部）：同样 5 条外链。
- 不新增本仓法务正文路由，不复制官网条款。

## ADMIN

- **仅外链（outbound only）**：登录页与后台页脚展示同一组官网链接。
- 不做“登录即同意”用户授权文案（管理员不接入用户授权）。
- 不新增本仓法务正文页。

## 验收（本仓范围）

- USER `/login` 可见 5 条指向 `www.bgssai.com/legal/*` 的双语链接。
- USER 登录后页脚可见同样 5 条。
- ADMIN 登录页与页脚可见同样 5 条，无同意授权句。
- 全仓检索无自造证照编号。
- UI 与文档标明 **需法务审阅**。

未执行：法务终稿、官网 5 页生产可用性、证照公示、用户真实同意留痕。
