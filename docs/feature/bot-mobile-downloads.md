# Bot 全平台安装包识别修复

需求版本：2026-09-17。

本应用下载目录已有 Android APK 或 iOS IPA 时，下载列表必须返回对应文件及 Android / iOS 平台标签，不能因为仅识别桌面扩展名而遗漏。Windows、macOS、Linux 既有行为保持。仅返回真实存在的文件；空目录不创建占位链接。AAB 是商店提交产物，不作为直接安装包列出。

下载入口、权限、响应字段沿用现有契约。本修复不代表签名、真机安装或商店上架已完成。

原型：[下载格式](../demo-static/web/user/bot-mobile-downloads.html)。设计：[格式识别](../api/bot-mobile-downloads.md)。
