# 测试用例：Chat third-party login (user PREP)

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/chat-oauth-prep.md`
- **功能 slug**: `chat-oauth-prep`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 4
- **生成用例数**: 4
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-CHAT-OAUTH-PREP-01 | AC-01 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：Date: 2026-09-11. Scope: bgssai-media user app only. | Date: 2026-09-11. Scope: bgssai-media user app only. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-CHAT-OAUTH-PREP-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：Center accounts live in bgssai-chat. Each product may use Chat as a third-party login **on the user | Center accounts live in bgssai-chat. Each product may use Chat as a third-party login **on the user | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-CHAT-OAUTH-PREP-03 | AC-03 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：app**. Admin never accepts Chat or user OAuth. This PR only prepares the user path | app**. Admin never accepts Chat or user OAuth. This PR only prepares the user path | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-CHAT-OAUTH-PREP-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：complete a live Chat authorization. | complete a live Chat authorization. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
