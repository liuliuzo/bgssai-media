# Bot 下载格式识别

对应 [需求版本 2026-09-17](../feature/bot-mobile-downloads.md)。现有下载列表接口、权限和 snake_case 响应结构不变：file_name、platform、size_bytes、updated_at、url。

目录配置仍为 bgssai.bot.download.directory。扩展名识别不区分大小写；在桌面格式基础上允许 .apk / .ipa，并优先归为 Android / iOS。AAB 与文本文件不进入下载列表。文件 URL 仍由本应用现有静态目录提供。

数据库不涉及：只扫描已有文件，不新增表、字段或种子。部署仍由 Jenkins 执行；dev / prod 均以 develop 为源，只切换既有 properties。

测试使用临时目录验证两类手机包被返回、平台正确、AAB 和 README 被排除，并保留原有桌面与空目录断言。
