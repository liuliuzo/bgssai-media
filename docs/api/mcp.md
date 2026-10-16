# API：MCP

基址：用户端后端（本机并发时多为 `http://127.0.0.1:8081`）。

统一业务响应仍为 `{ code, message, success, result }`（令牌管理接口）。
MCP JSON-RPC 响应为标准 `jsonrpc/id/result|error`，不包进 `ApiResponse`。

## 令牌管理（需登录）

请求头：`Jwttoken: <jwt>`，角色 `USER`。

### GET /api/mcp/tokens

返回当前用户未吊销令牌列表：

```json
{
  "code": 0,
  "message": "ok",
  "success": true,
  "result": [
    {
      "id": 1,
      "name": "Claude Desktop",
      "token_masked": "bm_mcp_abcd...ef12",
      "status": "active",
      "last_used_at": null,
      "created_at": "2026-09-10T00:00:00"
    }
  ]
}
```

### POST /api/mcp/tokens

Body：`{ "name": "Claude Desktop" }`

`result.token` 仅此响应出现一次。旧路径 `/api/mcp-tokens` 为过渡别名。

### POST /api/mcp/tokens/{id}/revoke

吊销后立即失效。

## MCP 端点

### GET /api/mcp

需 `Authorization: Bearer <pat>`。返回产品与传输说明 JSON。

### POST /api/mcp

Content-Type：`application/json`

Header：`Authorization: Bearer <pat>`

支持方法：

- `initialize`
- `ping`
- `tools/list`
- `tools/call`
- `notifications/initialized`（空结果确认）

`tools/call` 参数：

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "list_published_dramas",
    "arguments": { "keyword": "示例", "page_num": 1, "page_size": 10 }
  }
}
```

支持工具清单：
- `ping`：健康检查
- `list_published_dramas`：分页检索已发布剧集
- `get_drama`：获取已发布剧集及分集列表
- `list_continue_watching`：获取当前用户继续观看进度
- `list_my_videos`：获取当前创作者上传与草稿列表（支持 `status` 过滤）
- `create_video_draft`：创建创作者视频草稿（`title`, `description`, `category`, `cover_url`）
- `update_video_draft`：更新本人视频草稿元数据（`video_id`, `title`, `description`, `category`, `cover_url`）
- `get_upload_task_status`：查询本人视频转码与审核任务状态（`video_id` 或 `job_id`）
- `create_community_comment`：对社区视频或已发布短剧/长剧分集（`video_id` 或 `media_id` 如 `m_ep_701`）发表评论
- `toggle_community_like`：对社区视频或已发布短剧/长剧分集切换点赞状态

工具结果为 MCP `content[]` 文本（JSON 字符串），错误时 `isError=true`。

## 客户端配置摘要

Cursor（`.cursor/mcp.json`）：

```json
{
  "mcpServers": {
    "bgssai-media": {
      "url": "https://<user-host>/api/mcp",
      "headers": { "Authorization": "Bearer <pat>" }
    }
  }
}
```

Claude Desktop（经 `mcp-remote`）：

```json
{
  "mcpServers": {
    "bgssai-media": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "https://<user-host>/api/mcp",
        "--header",
        "Authorization: Bearer <pat>"
      ]
    }
  }
}
```

Codex / Grok Bot / bgssai-bot：同一 URL + Bearer PAT。
