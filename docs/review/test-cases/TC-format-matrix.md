# 测试用例：格式支持矩阵

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/format-matrix.md`
- **功能 slug**: `format-matrix`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 1
- **生成用例数**: 1
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-FORMAT-MATRIX-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：本地全格式文件，VLC 启发体验：权威 JSON：`bgssai-media-desktop/shared/format-matrix.json`。 ## 桌面端（必须覆盖） 视频容器：`.mp4` `.mkv` `.webm` `.mov` `.avi` `.flv` `.ts` / `.m3u8` `.mpg` `.wmv` `.3gp` 音频：`.mp | 本地全格式文件，VLC 启发体验：权威 JSON：`bgssai-media-desktop/shared/format-matrix.json`。 ## 桌面端（必须覆盖） 视频容器：`.mp4` `.mkv` `.webm` `.mov` `.avi` `.flv` `.ts` / `.m3u8` `.mpg` `.wmv` `.3gp` 音频：`.mp3` `.m4a` / `.aac` `.ogg` / `.opus` `.flac` `.wav` `.ape` / `.alac` `.wma` 视频编码：H.264/AVC、H.265/HEVC、AV1、VP9、MPEG-2 音频编码：AAC、MP3、Opus、FLAC、PCM ## Web 端（短剧分发） 仅渐进式浏览器友好格式：MP4（H.264/AAC）、WebM、HLS（m3u8）。 ## 验证 ```bash cd bg | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
