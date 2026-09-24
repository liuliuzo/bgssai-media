# bgssai-media 功能测试用例索引

| 项 | 值 |
| --- | --- |
| 仓库 | liuliuzo/bgssai-media |
| 分支 | develop |
| 用户端环境 | `待确认-dev` |
| 管理端环境 | `待确认-dev` |
| feature 目录文件盘点 | 13（含 meta / OOS） |
| 范围内功能（in-scope） | 13 |
| OUT-OF-SCOPE | 0 |
| 元文档（README/模板/愿景/总览） | 0 |
| 验收标准合计（in-scope） | 54 |
| 生成用例合计（in-scope） | 54 |
| 用例目录 | `TC-<slug>.md`（本目录） |
| 模板 | [`TEMPLATE.md`](./TEMPLATE.md) |
| 产品缩写（用例ID） | `MEDIA` |
| 编写日期 | 2026-09-24 |
| 执行状态 | [`STATUS.md`](./STATUS.md) |

## 功能清单

| 功能 slug | 标题 | 范围 | AC数 | TC数 | 用例文件 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| `MOBILE-IN-BOT-SHELL` | USER 前端：Bot 壳内窄栏触控布局（MVP） | in-scope | 8 | 8 | [TC-MOBILE-IN-BOT-SHELL.md](./TC-MOBILE-IN-BOT-SHELL.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `bot-mobile-downloads` | Bot 全平台安装包识别修复 | in-scope | 3 | 3 | [TC-bot-mobile-downloads.md](./TC-bot-mobile-downloads.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `chat-oauth-prep` | Chat third-party login (user PREP) | in-scope | 4 | 4 | [TC-chat-oauth-prep.md](./TC-chat-oauth-prep.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `format-matrix` | 格式支持矩阵 | in-scope | 1 | 1 | [TC-format-matrix.md](./TC-format-matrix.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `legal-fan-out` | 法务外链（官网权威页 fan-out） | in-scope | 5 | 5 | [TC-legal-fan-out.md](./TC-legal-fan-out.md) |  |
| `mcp` | MCP 连接器（对照 blog） | in-scope | 3 | 3 | [TC-mcp.md](./TC-mcp.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `media-01-commercial-gate` | MEDIA-01 Commercial Gate — Short-drama playback intake | in-scope | 5 | 5 | [TC-media-01-commercial-gate.md](./TC-media-01-commercial-gate.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `media-02-long-contract` | MEDIA-02 — Long contract + SourceRefs | in-scope | 3 | 3 | [TC-media-02-long-contract.md](./TC-media-02-long-contract.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `player` | 功能：播放器 | in-scope | 7 | 7 | [TC-player.md](./TC-player.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `short-drama-ingest` | Short drama ingest (shared contract) | in-scope | 4 | 4 | [TC-short-drama-ingest.md](./TC-short-drama-ingest.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `short-drama` | 功能：短剧平台（MVP） | in-scope | 4 | 4 | [TC-short-drama.md](./TC-short-drama.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `six-platform-clients` | BGSSAI Media 六端客户端 | in-scope | 3 | 3 | [TC-six-platform-clients.md](./TC-six-platform-clients.md) | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `support-messaging` | 站内在线客服（聊优先 + 聊中提工单） | in-scope | 4 | 4 | [TC-support-messaging.md](./TC-support-messaging.md) |  |

## 验收标准不够清晰 / 需推导的功能

- `MOBILE-IN-BOT-SHELL`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `bot-mobile-downloads`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `chat-oauth-prep`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `format-matrix`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `mcp`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `media-01-commercial-gate`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `media-02-long-contract`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `player`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `short-drama-ingest`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `short-drama`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `six-platform-clients`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。

## OUT-OF-SCOPE

- （无）

## 说明

1. 用例由 `docs/feature/*.md` 验收标准 1:1 展开，并补充明显负向/边界。
2. HTML 流程图不单独建用例，归属对应 `.md` 功能。
3. 文件中不含口令、JWT、PAT、短信密钥等密钥。
4. 真机 / 真实支付 / 真实三方 OAuth 未就绪项在备注标 **PENDING**。
5. 本套用例落库路径：`docs/review/test-cases/`（本仓）。执行结果填入各 `TC-*.md` 的「结果」列，并同步 [`STATUS.md`](./STATUS.md)。
