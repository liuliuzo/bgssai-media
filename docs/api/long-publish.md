# API：bgssai-long → bgssai-media 发布摄入（MEDIA-02）

权威契约：[docs/contracts/long-to-media-publish.md](../contracts/long-to-media-publish.md)。

## 摄入

`POST /bgssai/user/media/ingest/long-drama`

Headers：`X-Bgssai-Ingest-Token`（或 Bearer / legacy `X-Ingest-Token`）；可选 `Idempotency-Key`。

Body：见 schema `docs/contracts/long-drama-publish.schema.json`。

## 目录 / 播放

公开：

- `GET /bgssai/user/media/longs`
- `GET /bgssai/user/media/longs/{media_id}`
- `GET /bgssai/user/media/longs/works/{source_work_id}/episodes`

用户 JWT：

- `GET /api/longs`
- `GET /api/longs/{media_id}`
- `GET /api/longs/works/{source_work_id}/episodes`
