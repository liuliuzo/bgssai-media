# 功能：短剧平台（MVP）

## 数据

- drama：标题、封面、简介、状态 draft/published、source=manual|short_publish、external_ref
- episode：drama_id、ep_no、title、duration_sec、media_url/storage_key、status
- watch_progress：可选，继续观看 stub

## 用户端

首页已发布剧集 → 详情+分集列表 → 分集播放 → 继续观看入口（stub）

Short 契约目录：`/shorts` 列表 → `/shorts/{media_id}` 用 UniversalPlayer 播放 READY 条目。

## 管理端

剧集/分集 CRUD、发布/下架、查看 short 摄入日志
