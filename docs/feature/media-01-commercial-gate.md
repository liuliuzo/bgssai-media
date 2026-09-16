# MEDIA-01 Commercial Gate — Short-drama playback intake

Date: 2026-09-11, asset probe added 2026-09-16. Scope: bgssai-media short/long ingest → playable claim.

## Goal

Advance short-drama playback intake toward commercial readiness without faking playable success.

## Status model (ingest log / contract result)

| Status | Meaning | Playable? |
| --- | --- | --- |
| `PENDING` | Accepted, but playability is **not yet known**: the probe timed out, the origin returned 5xx/429, the probe is disabled, or only a `storage_key` was supplied. Retryable — the result carries `retryable=true`. | No |
| `FAILED` | Storage unconfigured, asset missing, or the probe proved the asset unusable (404/410, 401/403, HTML page, undecodable bytes). Terminal for that URL. | No |
| `READY` | Storage configured **and** the first bytes of the asset were fetched and recognised as a media container | Yes |

Only `READY` may expose `play_url` / `playable=true`. Controllers return `success=false` (HTTP body `code=422`) when ingest ends `FAILED`.

## Fail-closed rules

1. **Unconfigured storage** (`bgssai.media.storage.mode` blank, unsupported, or OBS missing endpoint/bucket/keys) → `FAILED`. No drama/episode publish. No `play_url`.
2. **Missing asset** (REFERENCE without http(s) `video_url` / `media_url`; OBS without `storage_key` or URL; empty episode list on admin publish) → `FAILED`.
3. **Asset not playable** — the probe fetched the asset and it is one of: dead (404/410), not accessible (401/403, includes expired signed URLs), an HTML page served with 200, an empty body, or bytes matching no known container → `FAILED`.
4. **Asset not verified** — transport failure, origin 5xx/429/408, `bgssai.media.probe.enabled=false`, or `storage_key` with no playable URL → `PENDING`. Held under the same idempotency key so a retry updates that row instead of opening a second one, and no `media_id` / `play_url` is handed out.
5. Never map legacy `PUBLISHED` / `success` onto a playable claim for new ingests.

## Asset probe (2026-09-16)

Configuration alone cannot decide playability, so `IngestReadiness.evaluate(gate, probe, url, key)`
fetches the asset before allowing READY. The probe is a **ranged GET**, not a HEAD: object stores and
CDNs answer HEAD from metadata that was never validated, and a HEAD cannot reveal that the body is an
HTML login wall. The range request costs ~8 KiB and, as a side effect, shows whether the origin
honours byte ranges — which is exactly what lets the player seek and resume.

The container is decided from the bytes (`MediaContainerSniffer`), not from `Content-Type`, because
publishers routinely send `application/octet-stream` for real video and `text/html` for an error page.
Recognised: mp4/ISO-BMFF, Matroska/WebM, MPEG-TS, AVI, WAV, FLV, Ogg, FLAC, ASF/WMV, MP3/ADTS,
HLS (`#EXTM3U`) and DASH (`<MPD>`).

```
bgssai.media.probe.enabled=true            # false => PENDING, never READY
bgssai.media.probe.sniff-bytes=8192
bgssai.media.probe.connect-timeout-ms=4000
bgssai.media.probe.request-timeout-ms=10000
```

Terminal and retryable verdicts are kept apart on purpose: a timeout says nothing about the asset and
must not burn the publisher's retry, while a 404 is a fact and should not be retried forever.

### What short / long should do with each status

| Result | Publisher action |
| --- | --- |
| `READY` | Publishable. `media_id` and `play_url` are returned. |
| `PENDING` + `retryable=true` | Re-submit the same `idempotency_key` after the origin recovers, or supply a signed `media_url` for an OBS object. Do **not** mark the episode published. |
| `FAILED` | Fix the asset (re-render, re-upload, re-sign the URL) and submit again. Resubmitting the same URL will fail the same way. |

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
| Ingest status model PENDING/FAILED/READY with unit tests | DONE |
| Unconfigured storage / missing asset → explicit FAIL | DONE |
| Fake READY / fake playable blocked in contract + detail/list | DONE |
| Dead / private / HTML-masquerading / undecodable URL cannot reach READY | DONE (2026-09-16, `HttpMediaAssetProbeTest`) |
| Transport failure stays retryable PENDING and reuses the held log row | DONE (2026-09-16) |
| Range support recorded so seek/resume is not assumed | DONE (2026-09-16) |
| Live format sample matrix (desktop + web) against owned fixtures | PENDING |
| Installers / packaged desktop player distribution | PENDING |
| Real bgssai-short → media ingest → UniversalPlayer E2E | PENDING |
| OBS copy mode with real bucket credentials | PENDING |

## Honest non-claims

- Unit tests prove fail-closed status logic; they do **not** prove commercial playback.
- `READY` now means the origin served recognisable media bytes at probe time. It does **not** prove the
  whole file decodes end to end, that every renditions/track is present, or that a time-limited URL will
  still resolve at playback time. Assets behind expiring signed URLs must be copied into managed storage
  before their URL expires; the probe only records the state at ingest.
- The probe does not transcode and does not run ffprobe. Container recognition is a header check, so a
  truncated or corrupt-after-the-header file can still reach READY.
- Format matrix smoke and Short live publish are still **PENDING** (see `docs/feature/format-matrix.md` TODO).
- Live Short→Media→player E2E (SHORT-02) remains a separate acceptance and is not closed by this change.
