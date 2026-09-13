# bgssai-media 验收与同步记录

日期：2026-09-12。需求版本：2026-09-11。

基线：`origin/develop`；本轮分支 `cursor/short-ingest-idempotent-edcd`。

完整目标：[产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)。

## 结论

**PARTIAL**。本轮将 media 摄入对齐 short PR #54 的幂等发布：`idempotency_key` UNIQUE upsert，
READY 批准包可入目录，重放返回已有目录项、不写第二条。不得把文件存在或单测通过写成全功能通过。

仍为 **PENDING**：官网仓清单逐条对读、官网其余 3 页生产可用性、法务终稿、证照公示、用户同意留痕、
本 Agent 无法读取私有 `bgssai-short` 仓原文、真实 Short 工作室 → UniversalPlayer E2E、全格式 live 样本、
播放器安装包、完整 Chat OAuth、境内四家真实授权。

## 本轮（short → media 幂等摄入）

| 项 | 状态 |
| --- | --- |
| 既有摄入 `POST /bgssai/user/media/ingest/short-drama` | DONE（本仓） |
| 目录 `GET /api/shorts` + `GET /bgssai/user/media/shorts` | DONE（本仓；按 media_id 去重） |
| UNIQUE(`idempotency_key`) upsert + 重放返回已有项 | DONE（单测；线上索引 **NOT_VERIFIED**） |
| 接受 READY / APPROVED 包；拒绝未批准 | DONE（单测） |
| 契约文档对齐 short 幂等键 | DONE（本仓；short 仓 404 未复读 PR #54 diff） |
| `/shorts` 中英说明 | DONE（本仓 UI） |
| 真实 Short 发布 → 播放 E2E | PENDING |

契约：[docs/contracts/short-to-media-publish.md](../contracts/short-to-media-publish.md)。

## 上一轮（法务 URL 对齐官网权威页）

上一轮分支 `cursor/legal-outbound-urls-154c`。法务状态：**需法务审阅**。

## 本轮（法务 URL 对齐官网权威页）

| 项 | 状态 |
| --- | --- |
| USER 登录页 5 条中英外链 | DONE（本仓 UI；**需法务审阅**） |
| USER 页脚 5 条中英外链（桌面 + 壳内） | DONE（本仓 UI；**需法务审阅**） |
| ADMIN 仅外链，无用户授权句 | DONE（本仓 UI） |
| URL 与官网权威 slug 完全一致 | DONE（禁 `/legal/*` 与错误 host） |
| 不编造证照/许可证编号 | DONE（本仓未写编号） |
| 官网 checklist 原文对读 | **NOT READ**（`bgssai-website` GitHub 404） |
| 官网 5 页 live + 法务终稿 | PENDING（terms / privacy 已发布；其余 3 slug 待官网发布） |

说明：[docs/feature/legal-fan-out.md](../feature/legal-fan-out.md)。

## 既有（short-media 闭环，仍成立）

| 项 | 状态 |
| --- | --- |
| 共享契约 + JSON Schema + packager/client | DONE（本仓） |
| 摄入 fail-closed PENDING/FAILED/READY（既有） | DONE |
| 用户 `/api/shorts` + `/shorts` 列表/播放 UI | DONE（需登录 + READY 才可播） |
| 用户 Chat 第三方登录 PREP；admin 隔离 | DONE（回调不签发会话） |
| bgssai-short 仓 PR | 用户称 PR #54 已合并；本 Agent 仍无法 clone 私仓复核 |
| Short 真实发布 → 播放 E2E | PENDING |
| 全格式样本 live / 安装包 | PENDING |

契约：[docs/contracts/short-to-media-publish.md](../contracts/short-to-media-publish.md)。
short 落点说明：[docs/contracts/bgssai-short-implement.md](../contracts/bgssai-short-implement.md)。
Chat PREP：[docs/feature/chat-oauth-prep.md](../feature/chat-oauth-prep.md)。

## 执行结果

**PASS（单测范围）**：见本 PR CI / 本地 `mvn -B -ntp -pl bgssai-media-common,bgssai-media-user,bgssai-media-admin -am test`。

测试通过只覆盖上述命令，不代表所有功能或线上环境通过。未执行付费模型、短信/邮件投递、第三方用户授权、真实站点发布、手机安装或 Short 联调播放。

## 数据库与交付边界

- 脚本版本：`sql/DDL.sql` + `sql/DML.sql` + `sql/patch_short_drama_contract.sql` + `sql/patch_ingest_idempotency.sql` + `sql/patch_chat_oauth_prep.sql`（无密钥）。
- `uk_ingest_idempotency`：DDL 已含；存量库需执行 `patch_ingest_idempotency.sql`。
- 线上数据库执行状态：NOT_VERIFIED。
