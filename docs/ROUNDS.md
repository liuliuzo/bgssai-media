# MVP 迭代轮次进度

同一 PR：`cursor/media-mvp-bootstrap-41a0` → `develop`。

| Round | 目标 | 本仓落点 | 状态 |
| --- | --- | --- | --- |
| 1 | Skeleton + backends + SQL | `pom.xml`, `bgssai-media-{common,admin,user}`, `sql/*`, `PRODUCT_VISION.md`, `README.md`, `docs/BGSSAI-Standards.md` | 完成 |
| 2 | User web + short-drama slice | `bgssai-media-user/frontend`（登录/首页/详情/播放）, `bgssai-media-admin/frontend`（CRUD/摄入日志） | 完成 |
| 3 | Full-format libVLC player | `bgssai-media-desktop` + `docs/feature/format-matrix.md` + fixtures smoke | 完成 |
| 4 | Short publish + polish | `IngestController`, `docs/api/short-publish.md`, `/formats` 页 | 完成 |
| 5 | Harden & merge-ready | 构建绿灯、PR ready、无 WIP | 完成 |

说明：`bgssai-skeleton` / `bgssai-short` 对本 Agent 仍 404；获权后按远程再对齐一轮 Standards。

## Round 6（本 PR）

| 目标 | 落点 | 状态 |
| --- | --- | --- |
| 关闭 short→media 发布/播放契约 | `docs/contracts/*`, `ShortDramaPublishJob` | 完成（media 侧） |
| 用户 Short 目录/播放 | `/api/shorts`, `/shorts` UI | 完成 |
| Chat 登录预留 / admin 隔离 | `ChatOauthService`, admin `/api/auth/policy` | PREP |
| short 仓对等 PR | 无法 clone | **blocked** |
