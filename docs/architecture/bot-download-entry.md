# Bot 下载导航设计

对应需求：../feature/bot-download-entry.md（2026-09-26）。

在用户端认证界面外层使用 BotDownloadLayout，导航为语义化 nav，后接原有认证界面。沿用现有 Link / ShellLink 和语言上下文，不把推广卡定位规则带入新导航，不读取历史忽略标记。样式限定为 bot-download-*，保留原有表单及响应式品牌区。已在 Bot 内时沿用原有隐藏规则。

接口、鉴权、下载安装包协议与数据库不涉及，无 SQL 修改；不会以入口调整宣称安装包已经发布或全应用托管已验收。dev/prod 发布源仍为 develop，properties 分环境。

验证：前端构建；浏览器检查桌面、窄屏入口可见、无重叠、匿名路由以及既有语言/shell 跳转。
