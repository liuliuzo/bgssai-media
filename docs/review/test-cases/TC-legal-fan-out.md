# 测试用例：法务外链（官网权威页 fan-out）

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/legal-fan-out.md`
- **功能 slug**: `legal-fan-out`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-LEGAL-FAN-OUT-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：USER `/login` 可见 5 条指向上述 `www.bgssai.com` 权威 slug 的中英链接。 | USER `/login` 可见 5 条指向上述 `www.bgssai.com` 权威 slug 的中英链接。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 |  |
| TC-MEDIA-LEGAL-FAN-OUT-02 | AC-02 | P0 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：USER 登录后页脚可见同样 5 条。 | USER 登录后页脚可见同样 5 条。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 |  |
| TC-MEDIA-LEGAL-FAN-OUT-03 | AC-03 | P0 | admin | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开管理端 待确认-dev<br>2. 按验收标准执行：ADMIN 登录页与页脚可见同样 5 条，无同意授权句。 | ADMIN 登录页与页脚可见同样 5 条，无同意授权句。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 |  |
| TC-MEDIA-LEGAL-FAN-OUT-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：全仓检索无法务页使用 `/legal/*` 或非 `www.bgssai.com` host。 | 全仓检索无法务页使用 `/legal/*` 或非 `www.bgssai.com` host。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 |  |
| TC-MEDIA-LEGAL-FAN-OUT-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：UI 与文档标明 **需法务审阅**。 | UI 与文档标明 **需法务审阅**。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 |  |
