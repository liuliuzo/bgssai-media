# 里程碑

## MVP 迭代轮次（本 PR）

| Round | 内容 | 状态 |
| --- | --- | --- |
| 1 | Skeleton 风格 monorepo + admin/user 后端 + SQL + 愿景/README | 完成 |
| 2 | User Web 短剧垂直切片 + Admin CRUD/摄入日志 UI | 完成 |
| 3 | 桌面 libVLC 播放器 + 全格式矩阵 + fixtures smoke | 完成 |
| 4 | short-publish 契约/冒烟 + 格式矩阵页 + 文档 | 完成 |
| 5 | 构建绿灯、README、PR merge-ready | 完成 |

说明：`bgssai-skeleton` / `bgssai-short` 对本 Agent 仍为私有不可读；已请求授权。获权后将按远程 Standards/布局再同步一轮。

## 本轮（short → media 闭环）

| 项 | 状态 |
| --- | --- |
| 共享发布契约 + JSON Schema + packager/client | 完成（本仓） |
| media 摄入 / 列表 / UniversalPlayer 播放路径 | 完成（本仓 MVP） |
| 用户 Chat 第三方登录 PREP；admin 隔离 | 完成（不签发 Chat 会话） |
| bgssai-short 仓 publish API/job PR | 用户称 PR #54 已合并；本 Agent 仍无法 clone 私仓复核 |
| 真实 Short 工作室 → 播放 E2E | PENDING |

## 后续

- 获权后对齐 skeleton/short 权威 ApiResponse/目录细项，并在 short 打开对等 PR
- Electron 打包安装器；更深 libVLC 嵌入（非仅 RC 外窗）
- 真实 OBS/SMS、完整 Chat OAuth、推荐
