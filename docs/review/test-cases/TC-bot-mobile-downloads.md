# 测试用例：Bot 全平台安装包识别修复

- **产品 / 仓库**: bgssai-media / liuliuzo/bgssai-media
- **功能文档**: `docs/feature/bot-mobile-downloads.md`
- **功能 slug**: `bot-mobile-downloads`
- **分支 / 版本**: develop
- **范围**: in-scope
- **验收标准数**: 3
- **生成用例数**: 3
- **环境**: User `待确认-dev` · Admin `待确认-dev`
- **编写人 / 日期**: Grok Bot / 2026-09-24
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-MEDIA-BOT-MOBILE-DOWNLOAD-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：本应用下载目录已有 Android APK 或 iOS IPA 时，下载列表必须返回对应文件及 Android / iOS 平台标签，不能因为仅识别桌面扩展名而遗漏 | 本应用下载目录已有 Android APK 或 iOS IPA 时，下载列表必须返回对应文件及 Android / iOS 平台标签，不能因为仅识别桌面扩展名而遗漏 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-BOT-MOBILE-DOWNLOAD-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：Windows、macOS、Linux 既有行为保持 | Windows、macOS、Linux 既有行为保持 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
| TC-MEDIA-BOT-MOBILE-DOWNLOAD-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 待确认-dev<br>2. 按验收标准执行：AAB 是商店提交产物，不作为直接安装包列出 | AAB 是商店提交产物，不作为直接安装包列出 | 占位账号 / 样例数据（无口令） | User:待确认-dev / Admin:待确认-dev | 待测 | 推导自原文 |
