# OPC 移动端对接接口（2026-10-17）

本仓 user API 供 `bgssai-media-android` / `bgssai-media-ios` / PC 桌面共用。统一响应 `{ code, message, success, result }`，鉴权头 `Jwttoken`。

媒体应用不提供 AI / 模型 / 额度套餐接口。

## 账号

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/account/deletion-notice` | 注销后果说明 |
| POST | `/api/account/delete` | body: `password` 或 `email_code`/`phone_code`，`acknowledged=true` |
| GET | `/api/account/bindings` | 登录方式列表 |
| POST | `/api/account/bindings` | 绑定：`type=email|phone|wechat|...` + 对应字段 |
| DELETE | `/api/account/bindings` | 解绑；最后一种方式拒绝 |

## 创作者作品

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| PATCH | `/api/creator/videos/{id}` | 编辑 `title`/`description`/`tags`/`visibility` |
| POST | `/api/creator/videos/{id}/unpublish` | 下架（visibility=private） |
| GET | `/api/creator/analytics/videos/{id}?from=&to=` | 按日分析 |

## 社区

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST/DELETE | `/api/community/channel/{id}/follow` | 订阅/取消 |
| GET | `/api/community/feed/subscriptions` | 订阅时间线 |
| GET | `/api/community/channel/{id}/videos` | 频道作品列表 |
| GET | `/api/community/search?q=` | 按标题/频道/标签 |
| POST | `/api/community/videos/{id}/not-interested` | 不感兴趣 |
| POST/DELETE | `/api/community/comments/{id}/pin` | 创作者置顶/取消 |
| POST | `/api/community/videos/{id}/comments` | 支持 `parent_id` 回复 |
| GET | `/api/community/videos/{id}/share` | 分享链接 |
| POST | `/api/community/videos/{id}/view-event` | 播放事件 |
| POST | `/api/community/reports` | `target_type` VIDEO/COMMENT + `target_id` + `reason` |

## 片库同步

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/library/history` | 观看历史；POST 写入 |
| DELETE | `/api/library/history/{videoId}` | 删除单条 |
| GET/PUT | `/api/library/history/pause` | 暂停记录 |
| GET/POST/DELETE | `/api/library/watch-later` | 稍后观看 |
| GET/POST | `/api/library/playlists` | 播放列表 |
| GET/POST/DELETE | `/api/library/playlists/{id}/items[...]` | 列表条目 |

## 管理端审核

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/reports` | 待处理举报 |
| POST | `/api/admin/reports/{id}/resolve` | `status=resolved|rejected` |
| POST | `/api/admin/videos/{id}/moderation` | `status=approved|rejected|pending` |

## SQL

- 全量：`sql/DDL.sql`
- 升级：`sql/upgrade/20261017_opc_requirements.sql` 与 `sql/dev/20261017_opc_requirements.sql`（同内容，非破坏性）
