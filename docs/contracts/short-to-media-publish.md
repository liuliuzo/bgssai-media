# Contract: bgssai-short → bgssai-media publish / play

Canonical shared contract for the closed loop. short produces; media ingests, lists, and plays.
short does **not** host a distribution channel.

This file is the source of truth. Older notes in `docs/api/short-publish.md` and
`docs/feature/short-drama-ingest.md` defer here.

## Loop

```
bgssai-short finished episode
        │
        │  ShortDramaPublishJob.packageFinished(...)
        │  POST {media}/bgssai/user/media/ingest/short-drama
        │  header X-Bgssai-Ingest-Token
        ▼
bgssai-media ingest (PENDING / FAILED / READY)
        │
        │  READY only → play_url
        ▼
user catalog  GET /api/shorts
user play     GET /api/shorts/{media_id}  → UniversalPlayer
admin logs    GET /api/ingest/logs
```

## Ingest (server to server)

`POST /bgssai/user/media/ingest/short-drama`

Headers (first match wins):

- `X-Bgssai-Ingest-Token` (preferred)
- `Authorization: Bearer <token>`
- `X-Ingest-Token` (legacy)

Token source: `MEDIA_INGEST_TOKEN` env, else `bgssai.media.ingest.token` in
`application-*.properties`. Do not commit real tokens. Outside `local`, blank token → `code=503`.

JSON body is snake_case. Schema: [short-drama-publish.schema.json](short-drama-publish.schema.json).

```json
{
  "source_system": "bgssai-short",
  "source_work_id": "w1",
  "source_episode_id": "e1",
  "source_film_id": "f1",
  "title": "Demo Short Episode 1",
  "cover_url": "https://example.com/cover.jpg",
  "video_url": "https://example.com/ep1.mp4",
  "duration_sec": 15,
  "aspect_ratio": "9:16",
  "language": "zh-CN",
  "tags": ["demo"],
  "idempotency_key": "short:w1:e1:f1"
}
```

`idempotency_key` MUST be `short:{source_work_id}:{source_episode_id}:{source_film_id}`.

Response `{ code, message, success, result }`:

- READY: `result.media_id`, `result.play_url`, `result.status=READY`, `result.playable=true`
- FAILED: HTTP body `code=422`, `success=false`, no `play_url` (unconfigured storage or missing asset)
- Duplicate key: upsert metadata / URL, same `media_id`

## Catalog / play

Public contract (for smoke / MCP):

- `GET /bgssai/user/media/shorts?q=&page=1&page_size=20`
- `GET /bgssai/user/media/shorts/{media_id}`

User app (JWT `Jwttoken`, role USER):

- `GET /api/shorts`
- `GET /api/shorts/{media_id}`

Only READY items are listed as playable. Detail hides `play_url` unless READY.

## Mapping

| Contract | Storage |
| --- | --- |
| work | `media_drama.external_ref = short:work:{source_work_id}` |
| episode / film | `media_episode` upsert `(drama_id, ep_no)`; `media_url=video_url`; `storage_key=film:{source_film_id}` |
| idempotency | `media_ingest_log.idempotency_key` UNIQUE |

Storage MVP: `REFERENCE` (`play_url = video_url`). No OBS copy. Does not prove the remote file exists.

## Short-side job (export)

Media ships the packager short should call or copy:

- `com.bgssai.media.common.publish.ShortDramaPublishJob`
- `com.bgssai.media.common.publish.ShortDramaPublishClient`

Suggested short API (not pushed — `bgssai-short` is private to this agent):

`POST /api/publish/media` (USER) with `{ source_work_id, source_episode_id, source_film_id }`
→ package finished metadata + asset refs → POST media ingest.

See [bgssai-short-implement.md](bgssai-short-implement.md).

## Auth boundary

- Ingest uses the shared ingest token, not a user JWT.
- Playback UI uses user JWT. Admin is a separate app and does not accept Chat / user OAuth.
- User Chat third-party login is PREP only (`/api/auth/chat/prepare`). Callback does not mint a session.

## Honest status

Unit tests cover packager + fail-closed ingest. Live Short studio → Media → player E2E is still PENDING
until `bgssai-short` is reachable and a real asset is published.
