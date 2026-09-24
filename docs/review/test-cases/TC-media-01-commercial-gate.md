# 测试用例：MEDIA-01 Commercial Gate — Short-drama playback intake

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/media-01-commercial-gate.md`
- **功能 slug**: `media-01-commercial-gate`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-MEDI-01-COMM-GATE-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：**Unconfigured storage** (`bgssai.media.storage.mode` blank, unsupported, or OBS missing endpoint/bucket/keys) → `FAILED`. No drama/episode publish. No `play_url`. | `FAILED`. No drama/episode publish. No `play_url`. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MEDI-01-COMM-GATE-02 | AC-02 | P1 | admin | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开管理端 待确认-dev<br>2. 按验收标准执行：**Missing asset** (REFERENCE without http(s) `video_url` / `media_url`; OBS without `storage_key` or URL; empty episode list on admin publish) → `FAILED`. | `FAILED`. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MEDI-01-COMM-GATE-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：**Asset not playable** — the probe fetched the asset and it is one of: dead (404/410), not accessible (401/403, includes expired signed URLs), an HTML page served with 200, an empt | `FAILED`. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MEDI-01-COMM-GATE-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：**Asset not verified** — transport failure, origin 5xx/429/408, `bgssai.media.probe.enabled=false`, or `storage_key` with no playable URL → `PENDING`. Held under the same idempoten | `PENDING`. Held under the same idempotency key so a retry updates that row instead of opening a second one, and no `media_id` / `play_url` is handed out. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MEDI-01-COMM-GATE-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：Never map legacy `PUBLISHED` / `success` onto a playable claim for new ingests. | Never map legacy `PUBLISHED` / `success` onto a playable claim for new ingests. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
