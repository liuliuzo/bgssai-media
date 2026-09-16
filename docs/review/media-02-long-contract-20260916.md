# MEDIA-02 review — Long contract + SourceRefs

日期：2026-09-16（Asia/Shanghai）。分支：`cursor/media-02-long-contract-source-refs`。

## 结论

**PARTIAL（契约 / 单测范围 PASS）**。本轮泛化 Short 来源标识为 `SourceRefs`，落地 Long 剧/集/版本/审核/幂等摄入与跨集查询。
真实 Long 工作室 → UniversalPlayer E2E：**NOT_VERIFIED**。

## 验收对照

| 项 | 状态 |
| --- | --- |
| Short 旧合同继续可用 | DONE（单测） |
| Long / Short 同名 ID 不冲突 | DONE（单测） |
| 跨集排序、同集新版本、重复提交、失败重试 | DONE（契约单测） |
| 真实播放 | NOT_VERIFIED |

## 契约字段摘要

- Short（不变）：`short:work:{work}`，`idempotency_key=short:{work}:{episode}:{film}`，`storage_key=film:{film}`
- Long：`long:work:{work}`，`idempotency_key=long:{work}:{episode}:{film}:v{version}`，`storage_key=long:film:{film}`
- Long 审核：`status` READY/APPROVED，`approved`，可选 `review_note`
- Long 查询：`/longs`，`/longs/{media_id}`，`/longs/works/{source_work_id}/episodes`（`ep_no` asc）

## 测试

`mvn -B -ntp -pl bgssai-media-common,bgssai-media-user -am test`（本 PR CI / 本地）。

测试通过只覆盖上述命令，不代表线上播放或 Long 联调通过。
