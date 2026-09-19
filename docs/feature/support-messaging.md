# 站内在线客服（留言 / 会话）

对齐组织标准：`BGSSAI 全产品在线客服标准（2026-09-19）`。  
本仓标注：`support=in-app messaging`。

## 能力

| 侧 | 能力 |
| --- | --- |
| 用户站 | 悬浮「在线客服」入口；创建会话（可匿名，尽量采集联系方式）；发文本；看历史；未读 |
| 管理站 | 会话列表 / 详情 / 回复 / 关闭 / 标记已读 |
| 六端壳 | 用户入口加载用户站后同入口可用；管理入口进处理台 |

状态：`open`（进行中）/ `pending`（待客服）/ `closed`（已关闭，用户可再开新会话）。

本阶段不做：第三方 SaaS 客服、强制 Bot 智能客服。

## 数据

- `media_support_session` / `media_support_message`（见 `sql/DDL.sql`、`sql/patch_support_messaging.sql`）
- 公开创建接口进程内限流（按用户或 IP）

## API（统一 `{ code, message, success, result }`）

### 用户（`bgssai-media-user`，`/api/support`）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/sessions` | 可选 JWT | 创建会话 + 首条留言；返回 `session`（含 `session_token`）与 `messages` |
| GET | `/sessions/mine` | USER | 我的会话列表 |
| GET | `/sessions/detail` | session_token 或归属用户 | 详情 |
| POST | `/sessions/messages` | 同上 | 发送留言 |
| POST | `/sessions/read` | 同上 | 用户侧已读 |

### 管理（`bgssai-media-admin`，`/api/support`，PLATFORM_ADMIN）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/sessions/page` | 分页列表（status / keyword） |
| GET | `/sessions/detail` | 详情 |
| POST | `/sessions/reply` | 回复 |
| POST | `/sessions/close` | 关闭 |
| POST | `/sessions/read` | 客服已读 |

## 验收

1. 用户打开悬浮入口 → 发留言 → 看到己方消息与会话。
2. 管理端列表可见 → 回复 → 用户侧可见。
3. 关闭后用户再发将开新会话。
