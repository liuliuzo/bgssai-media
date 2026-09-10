# API：MCP

基址：用户端后端（本机并发时多为 `http://127.0.0.1:8081`）。

统一业务响应仍为 `{ code, message, success, result }`（令牌管理接口）。
MCP JSON-RPC 响应为标准 `jsonrpc/id/result|error`，不包进 `ApiResponse`。

## 令牌管理（需登录）

请求头：`Jwttoken: <jwt>`，角色 `USER`。

### GET /api/mcp-tokens

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

### POST /api/mcp-tokens

Body：`{ "name": "Claude Desktop" }`

`result.token` 仅此响应出现一次。

### POST /api/mcp-tokens/{id}/revoke

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
