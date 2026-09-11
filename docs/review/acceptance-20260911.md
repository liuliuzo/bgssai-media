# bgssai-media 验收与同步记录

日期：2026-09-11。需求版本：2026-09-11。

基线：`origin/develop`（本轮 MEDIA-01 分支）；最终 commit/PR 见 GitHub。

完整目标：[产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)。

## 结论

**PARTIAL**。MEDIA-01 摄入侧已改为 fail-closed：`PENDING` / `FAILED` / `READY`；未配置存储或缺资产时明确 `FAILED`，禁止伪造可播（fake READY）。验证码真实投递改造此前已合入。境内第三方登录、全格式样本、播放器安装包、以及 **Short 真实发布 → 播放** 仍为本轮 **PENDING**，不得写成通过。

## MEDIA-01（本轮）

| 项 | 状态 |
| --- | --- |
| 摄入状态模型 PENDING/FAILED/READY + 单测 | DONE |
| 未配置存储 / 缺资产 → 显式 FAIL，无 play_url | DONE |
| 目录/详情仅 READY 可播；fake-ready 阻断 | DONE |
| 商业门禁文档 + 格式矩阵诚实 TODO | DONE |
| Jenkinsfile build+test | DONE |
| 全格式样本 live 验收 | PENDING |
| Short→Media→UniversalPlayer 真实 E2E | PENDING |
| 安装包分发 | PENDING |

详见 [media-01-commercial-gate.md](../feature/media-01-commercial-gate.md)。

## 执行结果

**PASS（单测范围）**：`mvn -B -ntp -pl bgssai-media-common,bgssai-media-user,bgssai-media-admin -am test`

`{'tests': 31, 'failures': 0, 'errors': 0, 'skipped': 0}`（bgssai-media-common）

测试通过只覆盖上述命令，不代表所有功能或线上环境通过。未执行付费模型、短信/邮件投递、第三方用户授权、真实站点发布、手机安装或 Short 联调播放。

## 五层产出定位

| 层 | 纳入指纹的文件数 | 状态 |
| --- | ---: | --- |
| requirements | 9 | 已记录内容指纹，语义按本报告复验 |
| prototype | 0 | 无该类文件，不可视为已完成 |
| design | 5 | 已记录内容指纹，语义按本报告复验 |
| database | 4 | 已记录内容指纹，语义按本报告复验 |
| code | 185+ | 本轮 MEDIA-01 代码变更需复验指纹后再更新快照 |

检查器唯一实现位于骨架仓 `scripts/verify_artifact_sync.py`。本仓快照为 [artifact-baseline.json](artifact-baseline.json)。指纹检查发现新增/删除/修改后必须复验；更新快照不能自动提升功能结论。

## 源码及契约证据

- [docs/feature/media-01-commercial-gate.md](../feature/media-01-commercial-gate.md)
- [bgssai-media-common/.../ingest/IngestStatus.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/ingest/IngestStatus.java)
- [bgssai-media-common/.../ingest/MediaStorageGate.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/ingest/MediaStorageGate.java)
- [bgssai-media-common/.../service/ShortDramaContractService.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/service/ShortDramaContractService.java)
- [bgssai-media-common/.../service/IngestService.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/service/IngestService.java)
- [bgssai-media-user/frontend/src/data/format-matrix.json](../../bgssai-media-user/frontend/src/data/format-matrix.json)
- [bgssai-media-user/frontend/src/components/UniversalPlayer/index.tsx](../../bgssai-media-user/frontend/src/components/UniversalPlayer/index.tsx)
- [Jenkinsfile](../../Jenkinsfile)

## 数据库与交付边界

产品源码/SQL 与验收快照在本分支记录；线上数据库执行状态为 NOT_VERIFIED。已知缺口（格式 live、Short E2E、安装包）不因提交 PR 而关闭。
