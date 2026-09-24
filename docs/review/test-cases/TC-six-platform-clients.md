# 测试用例：BGSSAI Media 六端客户端

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/six-platform-clients.md`
- **功能 slug**: `six-platform-clients`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 3
- **生成用例数**: 3
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-SIX-PLATFORM-CLIENTS-01 | AC-01 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：**桌面**：Electron；本地 `renderer/gate.html` 闸门；再加载 user/admin Web。 | **桌面**：Electron；本地 `renderer/gate.html` 闸门；再加载 user/admin Web。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SIX-PLATFORM-CLIENTS-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：**移动**：Capacitor；`www` 闸门；再打开 user（3002）/ admin（3001）或部署 URL。 | **移动**：Capacitor；`www` 闸门；再打开 user（3002）/ admin（3001）或部署 URL。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SIX-PLATFORM-CLIENTS-03 | AC-03 | P0 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：**小程序**：微信原生；首页闸门；用户/管理登录页 + 可选 web-view。 | **小程序**：微信原生；首页闸门；用户/管理登录页 + 可选 web-view。 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
