# Short drama ingest (shared contract)

Extends the merged MVP scaffold (`media_drama` / `media_episode` / `media_ingest_log`) with the **bgssai-short shared publish contract**.

Legacy admin path remains: `POST /api/ingest/short/publish` + `X-Ingest-Token` (also accepts `X-Bgssai-Ingest-Token`).

## Shared contract (MUST)

`POST /bgssai/user/media/ingest/short-drama`

Headers:

- `X-Bgssai-Ingest-Token: <token>` (preferred)
- or `Authorization: Bearer <token>`
- or legacy `X-Ingest-Token`

Token source (priority): `MEDIA_INGEST_TOKEN` env → `bgssai.media.ingest.token` in `application-*.properties`.
Outside `local`, blank token config → `code=503`.

### Body (snake_case)

```json
{
  "source_system": "bgssai-short",
  "source_work_id": "...",
  "source_episode_id": "...",
  "source_film_id": "...",
  "title": "...",
  "cover_url": "optional",
  "video_url": "https://... playable mp4",
  "duration_sec": 0,
  "aspect_ratio": "9:16",
  "language": "zh-CN",
  "tags": [],
  "idempotency_key": "short:{work_id}:{episode_id}:{film_id}"
}
```

### Response

```json
{
  "code": 0,
  "message": "ok",
  "success": true,
  "result": { "media_id": "m_ep_...", "play_url": "https://...", "status": "PUBLISHED" }
}
```

## Mapping onto MVP tables

| Contract | Storage |
| --- | --- |
| work | `media_drama.external_ref = short:work:{source_work_id}` |
| episode/film | `media_episode` upsert by `(drama_id, ep_no)`；`media_url=video_url`，`storage_key=film:{source_film_id}` |
| idempotency | `media_ingest_log.idempotency_key` UNIQUE；`media_id` / `play_url` columns |

Re-ingest same `idempotency_key` updates title/`video_url` and returns the same `media_id`.

## Catalog

- `GET /bgssai/user/media/shorts?q=&page=1&page_size=20`
- `GET /bgssai/user/media/shorts/{media_id}`

Also available via existing authenticated drama feed UI after ingest.

## Storage

REFERENCE: `play_url = video_url` (no OBS copy).

## Verify (rounds)

```bash
TOKEN=local-ingest-token-change-me
BASE=http://127.0.0.1:8080

# Round 1 happy path
curl -sS -X POST "$BASE/bgssai/user/media/ingest/short-drama" \
  -H "Content-Type: application/json" \
  -H "X-Bgssai-Ingest-Token: $TOKEN" \
  -d '{
    "source_system":"bgssai-short","source_work_id":"w1","source_episode_id":"e1","source_film_id":"f1",
    "title":"Demo Short Episode 1",
    "cover_url":"https://picsum.photos/seed/bgssai-media/720/1280",
    "video_url":"https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
    "duration_sec":15,"aspect_ratio":"9:16","language":"zh-CN","tags":["demo"],
    "idempotency_key":"short:w1:e1:f1"
  }'

# Round 2 bad token + re-ingest
curl -sS -X POST "$BASE/bgssai/user/media/ingest/short-drama" \
  -H "Content-Type: application/json" -H "X-Bgssai-Ingest-Token: wrong" \
  -d '{"source_system":"bgssai-short","source_work_id":"w1","source_episode_id":"e1","source_film_id":"f1","title":"x","video_url":"https://x","idempotency_key":"short:w1:e1:f1"}'

curl -sS -X POST "$BASE/bgssai/user/media/ingest/short-drama" \
  -H "Content-Type: application/json" -H "X-Bgssai-Ingest-Token: $TOKEN" \
  -d '{
    "source_system":"bgssai-short","source_work_id":"w1","source_episode_id":"e1","source_film_id":"f1",
    "title":"Demo Short Episode 1 (updated)",
    "video_url":"https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
    "duration_sec":16,"aspect_ratio":"9:16","language":"zh-CN","tags":["demo"],
    "idempotency_key":"short:w1:e1:f1"
  }'

# Round 3 list/detail
curl -sS "$BASE/bgssai/user/media/shorts?q=Demo"
curl -sS "$BASE/bgssai/user/media/shorts/<media_id>"
```

```bash
mvn -pl bgssai-media-common,bgssai-media-user -am test
```
