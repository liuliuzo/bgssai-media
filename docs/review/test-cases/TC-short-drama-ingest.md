# 测试用例：Short drama ingest (shared contract)

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/short-drama-ingest.md`
- **功能 slug**: `short-drama-ingest`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 4
- **生成用例数**: 4
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-SHORT-DRAMA-INGEST-01 | AC-01 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：Canonical write-up: [docs/contracts/short-to-media-publish.md](../contracts/short-to-media-publish.md). | Canonical write-up: [docs/contracts/short-to-media-publish.md](../contracts/short-to-media-publish.md). | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SHORT-DRAMA-INGEST-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：Extends the merged MVP scaffold (`media_drama` / `media_episode` / `media_ingest_log`) with the **bgssai-short shared publish contract**. | Extends the merged MVP scaffold (`media_drama` / `media_episode` / `media_ingest_log`) with the **bgssai-short shared publish contract**. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SHORT-DRAMA-INGEST-03 | AC-03 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：Legacy admin path remains: `POST /api/ingest/short/publish` + `X-Ingest-Token` (also accepts `X-Bgssai-Ingest-Token`). | Legacy admin path remains: `POST /api/ingest/short/publish` + `X-Ingest-Token` (also accepts `X-Bgssai-Ingest-Token`). | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SHORT-DRAMA-INGEST-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 待确认-dev 或管理端 待确认-dev<br>2. 按验收标准执行：`POST /bgssai/user/media/ingest/short-drama` | `POST /bgssai/user/media/ingest/short-drama` | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
