# bgssai-media 验收与同步记录

日期：2026-09-11。需求版本：2026-09-11。

基线：`6879b39c169b6ca29388e3ce8d94faed48a02e7e`（本轮 fetch 的 origin/develop）；本次修复在独立 codex 工作分支，最终 commit/PR 见 GitHub。

完整目标：[产品线验收基线](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)。

## 结论

**PARTIAL**。验证码发送已改为 VerifyCodeService，不再回显演示码；本轮后端测试通过。境内第三方登录、所有格式样本、播放器安装包与 Short 真实发布播放尚未完成本轮验收。

## 执行结果

**PASS**：mvn -B -ntp test；{'tests': 17, 'failures': 0, 'errors': 0, 'skipped': 0}

测试通过只覆盖上述命令，不代表所有功能或线上环境通过。浏览器控制工具本轮不可用；原型仅执行静态核对。未执行付费模型、短信/邮件投递、第三方用户授权、真实站点发布或手机安装。

## 五层产出定位

| 层 | 纳入指纹的文件数 | 状态 |
| --- | ---: | --- |
| requirements | 9 | 已记录内容指纹，语义按本报告复验 |
| prototype | 0 | 无该类文件，不可视为已完成 |
| design | 5 | 已记录内容指纹，语义按本报告复验 |
| database | 4 | 已记录内容指纹，语义按本报告复验 |
| code | 185 | 已记录内容指纹，语义按本报告复验 |

检查器唯一实现位于骨架仓 `scripts/verify_artifact_sync.py`。本仓快照为 [artifact-baseline.json](artifact-baseline.json)。指纹检查发现新增/删除/修改后必须复验；更新快照不能自动提升功能结论。

## 源码及契约证据

- [bgssai-media-user/frontend/src/data/format-matrix.json](../../bgssai-media-user/frontend/src/data/format-matrix.json)
- [bgssai-media-user/frontend/src/pages/UniversalPlayerPage.tsx](../../bgssai-media-user/frontend/src/pages/UniversalPlayerPage.tsx)
- [bgssai-media-user/frontend/src/components/UniversalPlayer/index.tsx](../../bgssai-media-user/frontend/src/components/UniversalPlayer/index.tsx)
- [bgssai-media-user/frontend/src/components/UniversalPlayer/UniversalPlayer.css](../../bgssai-media-user/frontend/src/components/UniversalPlayer/UniversalPlayer.css)
- [bgssai-media-common/src/main/java/com/bgssai/media/common/service/AuthService.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/service/AuthService.java)
- [bgssai-media-common/src/main/java/com/bgssai/media/common/service/VerifyCodeService.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/service/VerifyCodeService.java)
- [bgssai-media-common/src/main/java/com/bgssai/media/common/dto/ShortDramaIngestRequest.java](../../bgssai-media-common/src/main/java/com/bgssai/media/common/dto/ShortDramaIngestRequest.java)
- [bgssai-media-common/src/test/java/com/bgssai/media/common/service/VerifyCodeServiceTest.java](../../bgssai-media-common/src/test/java/com/bgssai/media/common/service/VerifyCodeServiceTest.java)

## 数据库与交付边界

产品源码/SQL 与验收快照在本分支记录；线上数据库执行状态为 NOT_VERIFIED。数据库目录为空的产品不能据此伪造 SQL，已有 JSON/Prisma 持久化应按真实实现评估。历史数据库批次保持只读，不把静态指纹称为已执行的迁移。

所有交付只走工作分支提交、push 和 draft PR；由用户在 GitHub 人工合并。已知缺口不因提交 PR 而关闭。
