# MCP 连接器（对照 blog）

让 Claude、Codex、Cursor、Grok Bot、bgssai-bot 连接 BGSSAI 媒体。

标语：**Connect your AI tools to bgssai**

## 能力边界（MVP 只读）

| 工具 | 说明 |
| --- | --- |
| `ping` | 连通性 |
| `list_published_dramas` | 已发布短剧分页检索 |
| `get_drama` | 已发布剧集 + 分集 |
| `list_continue_watching` | 当前 PAT 所属用户的继续观看 |

不做写入、不做摄入、不暴露中间件或第三方密钥。

## 用户操作

1. 登录用户端 → **设置**（`/settings`）
2. 在「MCP 接入」创建个人令牌（PAT），明文只显示一次
3. 在客户端配置端点与 Bearer 头（示例见设置页）

## 管理端

Admin → **MCP 连接器**（`/mcp-connectors`）为管理说明面：连接器清单、端点约定、工具列表。PAT 仍由用户自助签发（与 blog 一致）。

## 端点

- MCP：`POST /api/mcp`（用户端后端）
- 探测：`GET /api/mcp`（需 Bearer）
- 令牌：`/api/mcp-tokens`（需登录 `Jwttoken`，角色 `USER`）

鉴权：`Authorization: Bearer <pat>`

协议：Streamable HTTP JSON-RPC，`protocolVersion=2025-03-26`。

## 库表

`mcp_token`：只存 SHA-256；列表字段 `token_masked`。存量库执行 `sql/patch_mcp_token.sql`。

## 参考

- API 细节：`docs/api/mcp.md`
- 线上 blog 范式：用户设置「MCP 接入」+ `/api/mcp` + `/api/mcp-tokens`
