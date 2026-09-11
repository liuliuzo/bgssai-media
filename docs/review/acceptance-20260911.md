# bgssai-media 验收与同步记录

日期：2026-09-11。需求版本：2026-09-11。

基线：`origin/develop`；本轮分支 `cursor/short-media-publish-loop-15ac`。

完整目标：[产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)。

## 结论

**PARTIAL**。本轮补齐 **short → media 发布契约 + media 摄入/列表/播放路径（MVP）** 与 **用户 Chat 登录 PREP**。
不得把文件存在或单测通过写成全功能通过。

仍为 **PENDING**：short 仓对等 publish API（本 Agent 无法读取/推送 `bgssai-short`）、真实 Short 工作室 → UniversalPlayer E2E、全格式 live 样本、播放器安装包、完整 Chat OAuth、境内四家真实授权。

## 本轮（short-media 闭环）

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
