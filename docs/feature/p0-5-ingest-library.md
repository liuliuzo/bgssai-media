# P0-5 media: format table, library, idempotent publish

Date: 2026-10-12. Scope is this repository only.

## What this change closes

| Gate | State |
| --- | --- |
| Format matrix table with a verification column, plus URL hooks | DONE as a record. Every row is `NOT_VERIFIED`. |
| Browse surface for READY short and long ingest rows | DONE (`/library`, `GET /api/library`, `GET /bgssai/user/media/library`) |
| Idempotent short and long publish contract | Already on develop. Responses now include `format_hook`. Replay still returns the same `media_id`. |
| MEDIA host | UNDEPLOYED. See [docs/ops/media-hosts.md](../ops/media-hosts.md). |

## Format hook

`com.bgssai.media.common.format.FormatMatrix` reads `format/format-matrix.json`.

The same bytes are copied to:

- `bgssai-media-desktop/shared/format-matrix.json` (`player:matrix`, `player:formatHook`)
- `bgssai-media-user/frontend/src/data/format-matrix.json` (formats page and web player)

URL decision, locked by tests:

| Input | `web_decision` |
| --- | --- |
| `*.mp4` | `PLAY` |
| `*.mkv` | `REJECT` |
| `*.m3u8` | `PLAY` |
| no extension | `UNKNOWN` |

A REJECT does not fail ingest. Short and long may still store a desktop container as READY. The web player refuses it. `verification` stays `NOT_VERIFIED` until a sample is opened on that surface and the result is written down. This change does not contain that sample pass.

## Library

READY short rows and READY long rows are interleaved (recent short, recent long, and so on). `recommend` is that mix, capped at 8, and only when the caller did not filter by keyword or catalog. It is not a ranking model.

`kind` is `all`, `short`, or `long`. Each catalog read stops at 100 rows. `capped=true` when the underlying contract count is above that.

Long playback route: `/longs/{media_id}` → existing `GET /api/longs/{media_id}`.

## Publish

Unchanged paths:

- `POST /bgssai/user/media/ingest/short`
- `POST /bgssai/user/media/ingest/long`

`idempotency_key` stays UNIQUE. A second READY call returns `replayed=true` and the same `media_id`.

Smoke (needs a local user jar and database; it does not call a MEDIA host):

```bash
chmod +x scripts/publish-ingest-smoke.sh
MEDIA_BASE_URL=http://127.0.0.1:8081 ./scripts/publish-ingest-smoke.sh
```

## Still open

- No VLC fixture pass is recorded here, so the matrix is not a four-end sign-off.
- Android and iOS rows are `SHELL_ONLY` / `NOT_VERIFIED`.
- Live short or long studio → this API → picture and sound is NOT_VERIFIED.
- `MEDIA_USER_HOST` and `MEDIA_ADMIN_HOST` remain empty.
