# Contract: bgssai-long → bgssai-media publish / play (MEDIA-02)

Canonical shared contract for Long series ingest. long produces; media ingests, lists episodes, and plays.
long does **not** host a distribution channel.

Companion to [short-to-media-publish.md](short-to-media-publish.md). Source namespaces are shared via
`com.bgssai.media.common.source.SourceRefs`.

## Loop

```
bgssai-long finished + approved READY pack
        │
        │  LongDramaPublishJob.packageFinished(...)
        │  POST {media}/bgssai/user/media/ingest/long-drama
        │  header X-Bgssai-Ingest-Token
        │  idempotency_key = long:{work}:{episode}:{film}:v{version}
        ▼
bgssai-media ingest (PENDING / FAILED / READY)
        │  UNIQUE(idempotency_key) upsert
        │  work ref = long:work:{source_work_id}  (≠ short:work:…)
        │  same ep_no + new version → replace assets, stable media_id
        ▼
user catalog  GET /api/longs
user episodes GET /api/longs/works/{source_work_id}/episodes  (ep_no asc)
user play     GET /api/longs/{media_id}
public smoke  GET /bgssai/user/media/longs*
```

## Fields

| Field | Required | Notes |
| --- | --- | --- |
| `source_system` | yes | const `bgssai-long` |
| `source_work_id` | yes | Long work / project id |
| `source_episode_id` | yes | Digits preferred → `ep_no` |
| `source_film_id` | yes | Asset / export id |
| `source_version` | yes | Cut version; new version replaces same episode |
| `title` | yes | |
| `cover_url` | no | |
| `video_url` | no | blank → FAILED (missing asset) |
| `duration_sec` | no | default 0 |
| `aspect_ratio` | no | default `16:9` |
| `language` | no | default `zh-CN` |
| `tags` | no | |
| `status` | no | READY / APPROVED; omitted = approved READY |
| `approved` | no | `false` → 422 |
| `review_note` | no | recorded in payload |
| `idempotency_key` | yes | `long:{work}:{episode}:{film}:v{version}` |

## Mapping

| Contract | Storage |
| --- | --- |
| work | `media_drama.external_ref = long:work:{source_work_id}` UNIQUE |
| episode | `media_episode` upsert `(drama_id, ep_no)`; `storage_key=long:film:{source_film_id}` |
| version replace | new idempotency key; same `(drama_id, ep_no)` row updated; `media_id=m_ep_{episode.id}` stable |
| idempotency | `media_ingest_log.idempotency_key` UNIQUE; READY replay returns existing row |

Short uses `short:work:…` / `short:…:…:…` / `film:…` — identical upstream IDs never collide.

## Queries

- List READY long cuts: `GET /bgssai/user/media/longs` / `GET /api/longs` (filtered by `idempotency_key` prefix `long:`)
- Detail: `GET …/longs/{media_id}`
- Cross-episode sort: `GET …/longs/works/{source_work_id}/episodes` → `episodes[]` ordered by `ep_no` asc

## Example body

```json
{
  "source_system": "bgssai-long",
  "source_work_id": "w1",
  "source_episode_id": "e1",
  "source_film_id": "f1",
  "source_version": "2",
  "title": "Long Episode 1",
  "cover_url": "https://example.com/cover.jpg",
  "video_url": "https://example.com/ep1.mp4",
  "duration_sec": 2400,
  "aspect_ratio": "16:9",
  "language": "zh-CN",
  "tags": ["long"],
  "status": "READY",
  "approved": true,
  "review_note": "qc-pass",
  "idempotency_key": "long:w1:e1:f1:v2"
}
```

## Honest status

Unit / contract tests cover namespace isolation, cross-episode sort, version replace, duplicate replay, fail→retry.
Live Long studio → Media → player E2E: **NOT_VERIFIED** (no reachable long+media env in this PR).
