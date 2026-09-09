# BGSSAI 编码规范（媒体仓工作副本）

权威以 `bgssai-skeleton/docs/BGSSAI-Standards.md` 为准。本仓强制遵循摘要：

1. **配置**：环境差异下沉 `application-*.properties`；禁止 `${}` 占位符；中间件连接参数只写 properties。
2. **Controller**：单入口风格（按资源一个 Controller，方法扁平）；统一响应 `{ code, message, success, result }`。
3. **JSON**：snake_case。
4. **持久化**：MyBatis Example + PageHelper 物理分页；禁止 Lombok。
5. **鉴权**：JWT 请求头 `Jwttoken`；业务接口 `@NeedAop`；角色 `PLATFORM_ADMIN` / `USER`。
6. **登录**：user 支持密码 + 邮箱 OTP + 手机 OTP（可 stub）；admin 仅种子账号，不接 Chat。
7. **文档**：产品愿景/README/功能说明中文。
8. **禁止**：emoji、装饰性符号。
9. **业务第三方凭证**：Admin 维护 + 库表 + DML 种子（本 MVP 摄入令牌属服务间配置，写 properties 示例）。
