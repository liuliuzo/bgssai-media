# 测试用例：功能：播放器

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/player.md`
- **功能 slug**: `player`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 7
- **生成用例数**: 7
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-PLAYER-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：播放器对照 VLC，必须能播主流格式 | 播放器对照 VLC，必须能播主流格式 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-PLAYER-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：Web 端服务 YouTube / YouTube Shorts 式平台 | Web 端服务 YouTube / YouTube Shorts 式平台 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-PLAYER-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：完整容器/编码矩阵在桌面 libVLC | 完整容器/编码矩阵在桌面 libVLC | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-PLAYER-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：组件：`bgssai-media-user/frontend` UniversalPlayer（video.js + hls.js） | 组件：`bgssai-media-user/frontend` UniversalPlayer（video.js + hls.js） | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-PLAYER-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：支持：MP4 / WebM / HLS | 支持：MP4 / WebM / HLS | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-PLAYER-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：支持矩阵：见 `docs/feature/format-matrix.md` | 支持矩阵：见 `docs/feature/format-matrix.md` | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-PLAYER-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：UX：打开文件、播放列表、播放/暂停/停止、上一首/下一首、音量、倍速、seek | UX：打开文件、播放列表、播放/暂停/停止、上一首/下一首、音量、倍速、seek | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
