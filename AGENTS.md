# AGENTS.md

## 产品线验收基线（2026-09-11）

本次用户确认的完整需求与验收条目以 [骨架仓产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md) 为准。规划是目标，已实现和已验证状态以本仓 `docs/review/acceptance-20260911.md` 为准，不得把文件存在或单元测试通过写成全功能通过。

- BGSSAI 面向 OPC 创业用户，工具集合以 B2C 自助注册为主；官网介绍公司、产品和服务。
- Chat 提供 Web AI 对话，并作为可对外部应用开放的第三方登录提供方；应用建立自己的 user 会话。admin 管理员不接入用户授权。
- 境外仅 GEO / SaaS / Tokenhub / Chat 的 Global 仓。Web 用户登录需账号密码、邮箱验证码、手机验证码；境内另支持微信、抖音、百度、支付宝，境外另支持 Google、GitHub。
- Tokenhub CN/Global 分别接入大陆/国际模型原生 API，服务旗下产品和外部用户；Admin 模型凭证管理参考 Dify。
- 全产品支持 Bot 下载与授权托管；Wiki/Build 构成工程文档、HTML 设计、人工/Agent 编辑与编程任务的双向闭环。
- Short / Long 分别制作短剧和长剧；Media 参考 VLC，播放主流格式并接收 Short 成品发布。Wechat 参考既定 IM 项目，支持 Web/Android/iOS 及会话内 AI 图片和回复建议。Office 参考 OpenOffice，按实际格式与版本样本验收兼容性及 AI 办公。
- Blog 支持 AI、Markdown、富文本、HTML 发文；Note 对照小红书，两者推荐参考 x-algorithm。Publish 对照传声港；SaaS 包含工作流、CRM、线索与 Hootsuite 式社媒管理；Builder 支持预览、管理、编辑和 *.bgssai-builder.com 部署。
- 参考资料来自工作区 reference 目录；未完成实现、缺失参考和未执行的集成验收必须显式记录。数据库脚本版本与线上数据库执行状态分别报告。

## 本仓定位

**bgssai-media**：媒体播放器 + 短剧播放/分发平台。承接 `bgssai-short` 成品发布；short 不做分发渠道。

## 强制约定（摘要）

- 跨仓规范以 `docs/BGSSAI-Standards.md`（骨架仓权威副本）为准。
- 中间件连接参数只写 `application-*.properties` 字面量，禁止 `${}`。
- 统一响应 `{ code, message, success, result }`；JWT 头 `Jwttoken`；`@NeedAop`；角色 `PLATFORM_ADMIN` / `USER`。
- MyBatis Example + PageHelper；禁止 Lombok；JSON snake_case。
- user：密码 + 邮箱 OTP + 手机 OTP（真实投递、限时且一次性消费）；admin：仅 DML 种子，不接 Chat。
- 未登录默认进登录页。全局禁用 emoji。
- Git Flow：默认分支 `develop`；在 `feature/*` / `cursor/*` 分支工作，合入 develop 走 PR。

## 端口

配置默认 `server.port=8080`。本机同时跑 admin + user 时，user 用 `--server.port=8081`。前端 dev：admin 3001，user 3002。

愿景权威：`docs/PRODUCT-LINE-VISION.md` / 骨架仓 `PRODUCT-LINE-VISION.md`。


## 参考源（强制 GitHub remote）

优先从 GitHub 远程只读参考，勿依赖本机陈旧副本：

- https://github.com/liuliuzo/bgssai-skeleton
- https://github.com/liuliuzo/bgssai-short
- 本仓 https://github.com/liuliuzo/bgssai-media （唯一可写）

Standards / monorepo / auth / ApiResponse 以 skeleton、short 远程为准，变更后应重新 clone 同步。
