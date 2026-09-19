# 站内在线客服（聊优先 + 聊中提工单）

对齐组织标准：`BGSSAI 全产品在线客服标准（2026-09-19）` 及 UX update / Product model。  
本仓标注：`support=in-app messaging`（chat-first + ticket-from-chat）。

## 能力

| 侧 | 能力 |
| --- | --- |
| 用户站 | 悬浮「在线客服」；打开即气泡 + 底部输入；首条发送建会话（不强制姓名/邮箱/手机）；面板内「提工单」；未读 |
| 管理站 | 会话列表（含工单挂接/状态）/ 详情 / 回复 / 关闭 / 工单状态更新 |
| 六端壳 | 用户入口加载用户站后同入口可用；管理入口进处理台 |

会话状态：`open` / `pending` / `closed`。  
工单状态：`open` / `pending` / `resolved` / `closed`。工单是聊天的正式化升级，不替代聊天。

本阶段不做：第三方 SaaS 客服、强制 Bot 智能客服。

## 数据

- `media_support_session` / `media_support_message` / `media_support_ticket`
- 见 `sql/DDL.sql`、`sql/patch_support_messaging.sql`、`sql/patch_support_ticket.sql`
- 公开创建 / 发消息 / 提工单接口进程内限流（按用户或 IP）

## API（统一 `{ code, message, success, result }`）

### 用户（`bgssai-media-user`，`/api/support`）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/sessions` | 可选 JWT | 创建会话 + 首条留言；返回 `session` / `messages` / `ticket` |
| GET | `/sessions/mine` | USER | 我的会话列表 |
| GET | `/sessions/detail` | session_token 或归属用户 | 详情（含 ticket） |
| POST | `/sessions/messages` | 同上 | 发送留言 |
| POST | `/sessions/read` | 同上 | 用户侧已读 |
| POST | `/sessions/ticket` | 同上 | 聊中提工单（subject/priority/category 可选） |

### 管理（`bgssai-media-admin`，`/api/support`，PLATFORM_ADMIN）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/sessions/page` | 分页列表；每行 `{ session, ticket }` |
| GET | `/sessions/detail` | 详情（含 ticket） |
| POST | `/sessions/reply` | 回复 |
| POST | `/sessions/close` | 关闭 |
| POST | `/sessions/read` | 客服已读 |
| POST | `/sessions/ticket/status` | 更新工单状态 |

## 验收

1. 用户打开悬浮入口 → 直接见气泡区与输入框（无联系人表单门槛）→ 发留言 → 看到己方消息。
2. 会话中点「提工单」→ 工单挂接会话；可继续聊天。
3. 管理端列表可见工单挂接与状态 → 回复 → 用户侧可见；可更新工单状态。
4. 关闭后用户再发将开新会话。
