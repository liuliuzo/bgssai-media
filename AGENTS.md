# AGENTS.md

## 本仓定位

**bgssai-media**：媒体播放器 + 短剧播放/分发平台。承接 `bgssai-short` 成品发布；short 不做分发渠道。

## 强制约定（摘要）

- 跨仓规范以 `docs/BGSSAI-Standards.md`（骨架仓权威副本）为准。
- 中间件连接参数只写 `application-*.properties` 字面量，禁止 `${}`。
- 统一响应 `{ code, message, success, result }`；JWT 头 `Jwttoken`；`@NeedAop`；角色 `PLATFORM_ADMIN` / `USER`。
- MyBatis Example + PageHelper；禁止 Lombok；JSON snake_case。
- user：密码 + 邮箱 OTP + 手机 OTP（可 stub）；admin：仅 DML 种子，不接 Chat。
- 未登录默认进登录页。全局禁用 emoji。
- Git Flow：默认分支 `develop`；在 `feature/*` / `cursor/*` 分支工作，合入 develop 走 PR。

## 端口

配置默认 `server.port=8080`。本机同时跑 admin + user 时，user 用 `--server.port=8081`。前端 dev：admin 3001，user 3002。

愿景权威：`docs/PRODUCT-LINE-VISION.md` / 骨架仓 `PRODUCT-LINE-VISION.md`。
