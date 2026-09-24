# 测试用例：MEDIA-02 — Long contract + SourceRefs

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/media-02-long-contract.md`
- **功能 slug**: `media-02-long-contract`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 3
- **生成用例数**: 3
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-MEDI-02-LONG-CONT-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：Date: 2026-09-16. Scope: generalize Short source identifiers | Date: 2026-09-16. Scope: generalize Short source identifiers | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MEDI-02-LONG-CONT-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：define Long drama / episode / version / review / idempotency + queries. | define Long drama / episode / version / review / idempotency + queries. | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-MEDI-02-LONG-CONT-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：Contract: [docs/contracts/long-to-media-publish.md](../contracts/long-to-media-publish.md). | Contract: [docs/contracts/long-to-media-publish.md](../contracts/long-to-media-publish.md). | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
