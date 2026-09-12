# Contract: bgssai-short → bgssai-media publish / play

Canonical shared contract for the closed loop. short produces; media ingests, lists, and plays.
short does **not** host a distribution channel.

This file is the source of truth. Older notes in `docs/api/short-publish.md` and
`docs/feature/short-drama-ingest.md` defer here.

Aligned with **bgssai-short PR #54** (idempotent publish, already merged on short).
This agent cannot clone the private short repo; the media ingest contract below is
the matching receiver.

## Loop

```
bgssai-short finished + approved READY pack
        │
        │  ShortDramaPublishJob.packageFinished(...)
        │  POST {media}/bgssai/user/media/ingest/short-drama
        │  header X-Bgssai-Ingest-Token
        │  header Idempotency-Key (optional; body key wins when both present)
        │  idempotency_key = short:{work}:{episode}:{film}
        ▼
bgssai-media ingest (PENDING / FAILED / READY)
        │  UNIQUE(idempotency_key) upsert
        │  READY replay → existing catalog entry (no second row)
        │  READY only → play_url
        ▼
user catalog  GET /api/shorts
user play     GET /api/shorts/{media_id}  → UniversalPlayer
public smoke  GET /bgssai/user/media/shorts
admin logs    GET /api/ingest/logs
```

## Ingest (server to server)

`POST /bgssai/user/media/ingest/short-drama`

Headers (first match wins for the token):

- `X-Bgssai-Ingest-Token` (preferred)
- `Authorization: Bearer <token>`
- `X-Ingest-Token` (legacy)

Optional: `Idempotency-Key` fills `idempotency_key` when the body omits it.

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
  "status": "READY",
  "approved": true,
  "idempotency_key": "short:w1:e1:f1"
}
```

`idempotency_key` MUST be `short:{source_work_id}:{source_episode_id}:{source_film_id}`.

Media **accepts READY approved packs**:

- `status` omitted, `READY`, or `APPROVED` → accepted (then storage/asset gate still applies)
- `approved=false` or any other `status` (e.g. `DRAFT`) → `code=422`, no catalog write

Response `{ code, message, success, result }`:

- First READY write: `result.media_id`, `result.play_url`, `result.status=READY`,
  `result.playable=true`, `result.replayed=false`, `result.idempotency_key`
- READY replay (same key): **same catalog entry** — same `media_id` / `ingest_log_id` /
  `play_url`, `replayed=true`. No second `media_ingest_log` / drama / episode row.
- FAILED → READY (same key after a prior failure): upsert the existing log to READY
  (`replayed=false`). Still one catalog row.
- FAILED: HTTP body `code=422`, `success=false`, no `play_url` (unconfigured storage or missing asset)

## Catalog / play

Public contract (for smoke / MCP):

- `GET /bgssai/user/media/shorts?q=&page=1&page_size=20`
- `GET /bgssai/user/media/shorts/{media_id}`

User app (JWT `Jwttoken`, role USER):

- `GET /api/shorts`
- `GET /api/shorts/{media_id}`

Only READY items are listed as playable. Detail hides `play_url` unless READY.
List is unique by `media_id` so a duplicate publish cannot appear twice.

## Mapping

| Contract | Storage |
| --- | --- |
| work | `media_drama.external_ref = short:work:{source_work_id}` UNIQUE upsert |
| episode / film | `media_episode` upsert `(drama_id, ep_no)`; `media_url=video_url`; `storage_key=film:{source_film_id}` |
| idempotency | `media_ingest_log.idempotency_key` **UNIQUE**; replay of READY returns the existing row |

Storage MVP: `REFERENCE` (`play_url = video_url`). No OBS copy. Does not prove the remote file exists.

## SQL

- Fresh install: `sql/DDL.sql` already has `UNIQUE KEY uk_ingest_idempotency (idempotency_key)`.
- Existing DBs: `sql/patch_short_drama_contract.sql` and `sql/patch_ingest_idempotency.sql`
  add the unique index if missing. Multiple NULL keys remain allowed (legacy admin logs).
- Online execution: **NOT_VERIFIED**.

## Legacy admin path (not the short PR #54 contract)

`POST /api/ingest/short/publish` upserts by `external_ref` / `(drama_id, ep_no)`.
It does not write `media_id` and does not appear in `/api/shorts`.

## Short-side job (export)

Media ships the packager short should call or copy:

- `com.bgssai.media.common.publish.ShortDramaPublishJob` (sets `status=READY`, `approved=true`)
- `com.bgssai.media.common.publish.ShortDramaPublishClient`

Suggested short API (PR #54 on short; this agent cannot re-read the private repo):

`POST /api/publish/media` (USER) with `{ source_work_id, source_episode_id, source_film_id }`
→ package finished **READY approved** metadata + asset refs → POST media ingest
with the same `idempotency_key`. Retry is safe.

See [bgssai-short-implement.md](bgssai-short-implement.md).

## Auth boundary

- Ingest uses the shared ingest token, not a user JWT.
- Playback UI uses user JWT. Admin is a separate app and does not accept Chat / user OAuth.
- User Chat third-party login is PREP only (`/api/auth/chat/prepare`). Callback does not mint a session.

## Honest status

Unit tests cover packager + fail-closed ingest + UNIQUE replay. Live Short studio → Media → player E2E is still PENDING
until a real asset is published against a reachable media instance.
