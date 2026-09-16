# MEDIA-02 — Long contract + SourceRefs

Date: 2026-09-16. Scope: generalize Short source identifiers; define Long drama / episode / version / review / idempotency + queries.

## Done in this PR

| Item | State |
| --- | --- |
| `SourceRefs` shared namespace (`short` / `long`) | DONE |
| Short wire formats unchanged (`short:work:`, `short:w:e:f`, `film:`) | DONE |
| Long ingest `POST /bgssai/user/media/ingest/long-drama` | DONE |
| Long fields: work / episode / film / version / approved+status review / idempotency | DONE |
| Catalog + detail + episodes-by-work (`ep_no` asc) | DONE |
| Short catalog filtered to `long:` / `short:` prefixes separately | DONE |
| Contract tests: isolation, sort, version replace, replay, fail→retry | DONE |
| Live playback E2E | **NOT_VERIFIED** |

## Acceptance map

| Gate | Evidence |
| --- | --- |
| Short 旧合同继续可用 | `ShortDramaContractService` + existing tests; `shortOldContractStillWorksAfterSourceRefs` |
| Long 与 Short 同名 ID 不冲突 | `longAndShortSameUpstreamIdsDoNotConflict` |
| 跨集排序 | `crossEpisodeSortedByEpNo` |
| 同集新版本替换 | `newVersionReplacesSameEpisodePlayUrlKeepsMediaId` |
| 重复提交 | `duplicateSubmissionReplaysWithoutSecondInsert` |
| 失败重试 | `failedThenReadyUpgradesSameKey` |
| 真实播放 | NOT_VERIFIED |

Contract: [docs/contracts/long-to-media-publish.md](../contracts/long-to-media-publish.md).
