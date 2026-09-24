# 测试用例：USER 前端：Bot 壳内窄栏触控布局（MVP）

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/MOBILE-IN-BOT-SHELL.md`
- **功能 slug**: `MOBILE-IN-BOT-SHELL`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 8
- **生成用例数**: 8
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-MOBI-IN-BOT-SHEL-01 | AC-01 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：权威约定对齐骨架仓 `MOBILE-IN-BOT-SHELL.md` | 权威约定对齐骨架仓 `MOBILE-IN-BOT-SHELL.md` | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：本仓只改 **用户端 Web** | 本仓只改 **用户端 Web** | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-03 | AC-03 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：Admin 与桌面播放器跳过 | Admin 与桌面播放器跳过 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：任一查询参数即可进入壳模式： | 任一查询参数即可进入壳模式： | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-05 | AC-05 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：支持 `1` / `true` / `yes` | 支持 `1` / `true` / `yes` | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-06 | AC-06 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：无上述参数时保持现有桌面独立布局，不做媒体查询强制手机化 | 无上述参数时保持现有桌面独立布局，不做媒体查询强制手机化 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-07 | AC-07 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：播放器按栏宽自适应，避免横向滚动 | 播放器按栏宽自适应，避免横向滚动 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MOBI-IN-BOT-SHEL-08 | AC-08 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：http://localhost:3002/          # 桌面独立，应与改前一致 | http://localhost:3002/          # 桌面独立，应与改前一致 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
