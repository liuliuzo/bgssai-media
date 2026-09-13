# MEDIA-01 Commercial Gate — Short-drama playback intake

Date: 2026-09-11. Scope: bgssai-media short-drama ingest → playable claim.

## Goal

Advance short-drama playback intake toward commercial readiness without faking playable success.

## Status model (ingest log / contract result)

| Status | Meaning | Playable? |
| --- | --- | --- |
| `PENDING` | Accepted metadata; asset/storage work not finished (reserved for async OBS copy) | No |
| `FAILED` | Storage unconfigured, asset missing, or validation failed | No |
| `READY` | Storage configured **and** asset reference present | Yes |

Only `READY` may expose `play_url` / `playable=true`. Controllers return `success=false` (HTTP body `code=422`) when ingest ends `FAILED`.

## Fail-closed rules

1. **Unconfigured storage** (`bgssai.media.storage.mode` blank, unsupported, or OBS missing endpoint/bucket/keys) → `FAILED`. No drama/episode publish. No `play_url`.
2. **Missing asset** (REFERENCE without http(s) `video_url` / `media_url`; OBS without `storage_key` or URL; empty episode list on admin publish) → `FAILED`.
3. Never map legacy `PUBLISHED` / `success` onto a playable claim for new ingests.

## Storage configuration (middleware properties only)

```
bgssai.media.storage.mode=REFERENCE   # or OBS; blank = FAIL
bgssai.media.obs.endpoint=
bgssai.media.obs.bucket=
bgssai.media.obs.access-key=
bgssai.media.obs.secret-key=
```

REFERENCE mode is URL passthrough (`play_url = video_url`). It does **not** prove the remote file exists. Live Short→Media→player E2E remains a separate gate.

## Commercial gate checklist

| Gate | State |
| --- | --- |
| Ingest status model PENDING/FAILED/READY with unit tests | DONE (this PR) |
| Unconfigured storage / missing asset → explicit FAIL | DONE (this PR) |
| Fake READY / fake playable blocked in contract + detail/list | DONE (this PR) |
| Live format sample matrix (desktop + web) against owned fixtures | PENDING |
| Installers / packaged desktop player distribution | PENDING |
| Real bgssai-short → media ingest → UniversalPlayer E2E | PENDING |
| OBS copy mode with real bucket credentials | PENDING |

## Honest non-claims

- Unit tests prove fail-closed status logic; they do **not** prove commercial playback.
- `REFERENCE` + http(s) URL → `READY` means “configured reference present”, not “bytes verified on CDN”.
- Format matrix smoke and Short live publish are still **PENDING** (see `docs/feature/format-matrix.md` TODO).
