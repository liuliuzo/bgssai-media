# API：bgssai-short → bgssai-media 发布摄入

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

## 幂等

以 `external_ref` 为唯一键 upsert 剧集；分集按 `(drama_id, ep_no)` upsert。重复调用覆盖元数据与分集列表中出现的集。

## 响应

统一结构 `{ "code", "message", "success", "result" }`。成功时 `result` 含 `drama_id` 与摄入日志 id。

## short 侧调用提示（stub）

配置 media 基址与 ingest token，在成品「发布到渠道」时 POST 上述 payload。本 PR 不修改 bgssai-short 代码。

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
