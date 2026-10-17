# liuliuzo/bgssai-media-pc 存量需求覆盖索引

基线：`2b21ee58264ab026aa1b6ca5243501ccdfb98c83`；日期：2026-10-17。列出22个存量需求/索引文件。功能清单中的有效条款按专项用例保留；索引、模板、旧方案与审计材料标注用途，不当作独立产品能力。

历史套件匹配依据为需求路径/文件名或同名套件；这是按文件的测试设计导航，不等于每个旧条款已经运行通过。执行前须同时核对源文中的字段与所有参数组合。本轮逐条验收见[追溯矩阵](TRACEABILITY-20261017.md)。

| 来源 | 关联方式/范围 | 历史用例（结果未改写） | 本轮用例 |
| --- | --- | --- | --- |
| [账号入口与密码口径](../../docs/feature/account-entry-20260924.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-002](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-002-P, TC-MEDIA-PC-REGRESSION-002-N |
| [Bot 下载入口体验](../../docs/feature/bot-download-entry.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-COMMON-014](TC-opc-common-20261017.md)：TC-MEDIA-PC-COMMON-014-P, TC-MEDIA-PC-COMMON-014-N |
| [Bot 全平台安装包识别修复](../../docs/feature/bot-mobile-downloads.md) | 专项或共同契约用例 | [TC-bot-mobile-downloads.md](../../docs/review/test-cases/TC-bot-mobile-downloads.md) | [REQ-MEDIA-PC-COMMON-014](TC-opc-common-20261017.md)：TC-MEDIA-PC-COMMON-014-P, TC-MEDIA-PC-COMMON-014-N |
| [Chat third-party login (user PREP)](../../docs/feature/chat-oauth-prep.md) | 历史套件保留 | [TC-chat-oauth-prep.md](../../docs/review/test-cases/TC-chat-oauth-prep.md) | — |
| [格式支持矩阵](../../docs/feature/format-matrix.md) | 历史套件保留 | [TC-format-matrix.md](../../docs/review/test-cases/TC-format-matrix.md)<br>[TC-player.md](../../docs/review/test-cases/TC-player.md) | — |
| [协议入口去重（2026-09-29）](../../docs/feature/legal-entry-dedup.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-003](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-003-P, TC-MEDIA-PC-REGRESSION-003-N |
| [法务外链（官网权威页 fan-out）](../../docs/feature/legal-fan-out.md) | 历史套件保留 | [TC-legal-fan-out.md](../../docs/review/test-cases/TC-legal-fan-out.md) | — |
| [用户登录页整理（2026-09-26）](../../docs/feature/login-page-cleanup-20260926.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-002](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-002-P, TC-MEDIA-PC-REGRESSION-002-N |
| [MCP 连接器（对照 blog）](../../docs/feature/mcp.md) | 历史套件保留 | [TC-mcp.md](../../docs/review/test-cases/TC-mcp.md) | — |
| [MEDIA-01 Commercial Gate — Short-drama playback intake](../../docs/feature/media-01-commercial-gate.md) | 历史套件保留 | [TC-media-01-commercial-gate.md](../../docs/review/test-cases/TC-media-01-commercial-gate.md) | — |
| [MEDIA-02 — Long contract + SourceRefs](../../docs/feature/media-02-long-contract.md) | 历史套件保留 | [TC-media-02-long-contract.md](../../docs/review/test-cases/TC-media-02-long-contract.md) | — |
| [USER 前端：Bot 壳内窄栏触控布局（MVP）](../../docs/feature/MOBILE-IN-BOT-SHELL.md) | 历史套件保留 | [TC-MOBILE-IN-BOT-SHELL.md](../../docs/review/test-cases/TC-MOBILE-IN-BOT-SHELL.md) | — |
| [验证码登录即注册](../../docs/feature/otp-entry-20260924.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-002](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-002-P, TC-MEDIA-PC-REGRESSION-002-N |
| [P0-5 media: format table, library, idempotent publish](../../docs/feature/p0-5-ingest-library.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-006](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-006-P, TC-MEDIA-PC-REGRESSION-006-N |
| [功能：播放器](../../docs/feature/player.md) | 历史套件保留 | [TC-player.md](../../docs/review/test-cases/TC-player.md) | — |
| [用户端动态视觉（2026-09-25）](../../docs/feature/product-motion.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-004](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-004-P, TC-MEDIA-PC-REGRESSION-004-N |
| [功能需求](../../docs/feature/README.md) | 索引或模板，不作为独立业务功能 | — | — |
| [Short drama ingest (shared contract)](../../docs/feature/short-drama-ingest.md) | 历史套件保留 | [TC-short-drama-ingest.md](../../docs/review/test-cases/TC-short-drama-ingest.md) | — |
| [功能：短剧平台（MVP）](../../docs/feature/short-drama.md) | 历史套件保留 | [TC-short-drama.md](../../docs/review/test-cases/TC-short-drama.md) | — |
| [单账号当前会话](../../docs/feature/single-session-20260924.md) | 专项或共同契约用例 | — | [REQ-MEDIA-PC-REGRESSION-001](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-001-P, TC-MEDIA-PC-REGRESSION-001-N |
| [BGSSAI Media 六端客户端](../../docs/feature/six-platform-clients.md) | 历史套件保留 | [TC-six-platform-clients.md](../../docs/review/test-cases/TC-six-platform-clients.md) | — |
| [站内在线客服（聊优先 + 聊中提工单）](../../docs/feature/support-messaging.md) | 专项或共同契约用例 | [TC-support-messaging.md](../../docs/review/test-cases/TC-support-messaging.md) | [REQ-MEDIA-PC-REGRESSION-005](TC-feature-gaps-20261017.md)：TC-MEDIA-PC-REGRESSION-005-P, TC-MEDIA-PC-REGRESSION-005-N |
