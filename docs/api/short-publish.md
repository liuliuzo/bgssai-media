# API：bgssai-short → bgssai-media 发布摄入

**Canonical contract:** [docs/contracts/short-to-media-publish.md](../contracts/short-to-media-publish.md)
(schema: [short-drama-publish.schema.json](../contracts/short-drama-publish.schema.json)).

## 概述

short 侧成品短剧通过服务端调用本接口发布到 media。使用共享摄入令牌（配置项 `bgssai.media.ingest.token`，勿写入 git 真密钥；local 示例见 `application-local.properties`）。

## 端点

`POST /api/ingest/short/publish`

Header：

- `X-Ingest-Token: <token>`
- `Content-Type: application/json`

## 请求体（snake_case）

```json
{
  "external_ref": "short-drama-10001",
  "title": "示例短剧",
  "cover_url": "https://example.com/cover.jpg",
  "description": "简介",
  "status": "published",
  "episodes": [
    {
      "ep_no": 1,
      "title": "第1集",
      "duration_sec": 120,
      "media_url": "https://example.com/ep1.mp4",
      "storage_key": null,
      "status": "published"
    }
  ]
}
```

## 幂等（遗留 admin 路径）

以 `external_ref` 为唯一键 upsert 剧集；分集按 `(drama_id, ep_no)` upsert。重复调用覆盖元数据与分集列表中出现的集。
该路径不写 `media_id`，不进入 `/api/shorts` 契约目录。

short PR #54 使用的共享契约路径见 [short-to-media-publish.md](../contracts/short-to-media-publish.md)：
`idempotency_key` UNIQUE；READY 重放返回已有目录项。

## 响应

统一结构 `{ "code", "message", "success", "result" }`。成功时 `result` 含 `drama_id` 与摄入日志 id。

## short 侧调用提示

配置 media 基址与 ingest token，在成品「发布到 media」时 POST 共享契约 payload。
Packager: `ShortDramaPublishJob` / `ShortDramaPublishClient`。
short 仓实现说明（本 Agent 无法推送 short）：[bgssai-short-implement.md](../contracts/bgssai-short-implement.md)。

## 本机冒烟

```bash
curl -s -X POST http://127.0.0.1:8080/api/ingest/short/publish \
  -H 'Content-Type: application/json' \
  -H 'X-Ingest-Token: local-ingest-token-change-me' \
  -d '{
    "external_ref": "short-smoke-001",
    "title": "short 冒烟剧",
    "status": "published",
    "episodes": [
      {
        "ep_no": 1,
        "title": "EP1",
        "media_url": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "status": "published"
      }
    ]
  }'
```

Admin 端「入库日志」可看到本次摄入记录。


---

## Shared contract (preferred for short-side agent)

See `docs/feature/short-drama-ingest.md`.

`POST /bgssai/user/media/ingest/short-drama` + header `X-Bgssai-Ingest-Token`.
