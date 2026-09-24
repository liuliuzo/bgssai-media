# 测试用例：功能：短剧平台（MVP）

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/short-drama.md`
- **功能 slug**: `short-drama`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 4
- **生成用例数**: 4
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-SHORT-DRAMA-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：drama：标题、封面、简介、状态 draft/published、source=manual/short_publish、external_ref | drama：标题、封面、简介、状态 draft/published、source=manual/short_publish、external_ref | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SHORT-DRAMA-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：episode：drama_id、ep_no、title、duration_sec、media_url/storage_key、status | episode：drama_id、ep_no、title、duration_sec、media_url/storage_key、status | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SHORT-DRAMA-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：watch_progress：可选，继续观看 stub | watch_progress：可选，继续观看 stub | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-SHORT-DRAMA-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：首页已发布剧集 → 详情+分集列表 → 分集播放 → 继续观看入口（stub） | 详情+分集列表 → 分集播放 → 继续观看入口（stub） | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
