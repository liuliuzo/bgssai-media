# bgssai-media 验收与同步记录

日期：2026-09-12。需求版本：2026-09-11。

基线：`origin/develop`；本轮分支 `cursor/legal-fan-out-38bb`。

完整目标：[产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)。

## 结论

**PARTIAL**。本轮补齐 **USER 登录/页脚 + ADMIN 外链** 指向官网 5 条权威法务页（双语标签）。
不得把文件存在或单测通过写成全功能通过。法务状态：**需法务审阅**。

仍为 **PENDING**：官网仓清单逐条对读（本 Agent 无法读取 `bgssai-website`）、官网 5 页生产可用性、法务终稿、证照公示、用户同意留痕、short 仓对等 publish API、真实 Short 工作室 → UniversalPlayer E2E、全格式 live 样本、播放器安装包、完整 Chat OAuth、境内四家真实授权。

## 本轮（法务 fan-out）

| 项 | 状态 |
| --- | --- |
| USER 登录页 5 条双语外链 | DONE（本仓 UI；**需法务审阅**） |
| USER 页脚 5 条双语外链（桌面 + 壳内） | DONE（本仓 UI；**需法务审阅**） |
| ADMIN 仅外链，无用户授权句 | DONE（本仓 UI） |
| 不编造证照/许可证编号 | DONE（本仓未写编号） |
| 官网 checklist 原文对读 | **NOT READ**（`bgssai-website` GitHub 404） |
| 官网 5 页 live + 法务终稿 | PENDING |

说明：[docs/feature/legal-fan-out.md](../feature/legal-fan-out.md)。

## 既有（short-media 闭环，仍成立）

| 项 | 状态 |
| --- | --- |
| 共享契约 + JSON Schema + packager/client | DONE（本仓） |
| 摄入 fail-closed PENDING/FAILED/READY（既有） | DONE |
| 用户 `/api/shorts` + `/shorts` 列表/播放 UI | DONE（需登录 + READY 才可播） |
| 用户 Chat 第三方登录 PREP；admin 隔离 | DONE（回调不签发会话） |
| bgssai-short 仓 PR | **NOT PUSHED**（GitHub 404） |
| Short 真实发布 → 播放 E2E | PENDING |
| 全格式样本 live / 安装包 | PENDING |

契约：[docs/contracts/short-to-media-publish.md](../contracts/short-to-media-publish.md)。
short 落点说明：[docs/contracts/bgssai-short-implement.md](../contracts/bgssai-short-implement.md)。
Chat PREP：[docs/feature/chat-oauth-prep.md](../feature/chat-oauth-prep.md)。

## 执行结果

**PASS（单测范围）**：见本 PR CI / 本地 `mvn -B -ntp -pl bgssai-media-common,bgssai-media-user,bgssai-media-admin -am test`。

测试通过只覆盖上述命令，不代表所有功能或线上环境通过。未执行付费模型、短信/邮件投递、第三方用户授权、真实站点发布、手机安装或 Short 联调播放。

## 数据库与交付边界

- 脚本版本：`sql/DDL.sql` + `sql/DML.sql` + `sql/patch_short_drama_contract.sql` + `sql/patch_chat_oauth_prep.sql`（注释级，无密钥）。
- 线上数据库执行状态：NOT_VERIFIED。
