# USER 前端：Bot 壳内窄栏触控布局（MVP）

权威约定对齐骨架仓 `MOBILE-IN-BOT-SHELL.md`。本仓只改 **用户端 Web**；Admin 与桌面播放器跳过。

## 探测

任一查询参数即可进入壳模式：

- `bgssai_shell=1`
- `chat_pane=1`

支持 `1` / `true` / `yes`。写入 `sessionStorage` 后，SPA 跳转仍保持窄栏。`bgssai_shell=0` 或 `chat_pane=0` 退出壳模式。

无上述参数时保持现有桌面独立布局，不做媒体查询强制手机化。

## 壳内行为

- `html` 增加 `bgssai-shell-pane` 与 `data-bgssai-shell=1`
- 顶栏压缩；底栏触控 Tab（首页 / Short / 继续 / 播放 / 设置）
- 内容满栏宽，按钮与输入至少 44px
- 播放器按栏宽自适应，避免横向滚动
- 登录/Chat 回调卡片铺满窄栏

## 不做

- Admin 前端（管理员不进 Bot 用户授权）
- `bgssai-media-desktop` Electron
- 改鉴权、密钥或中间件配置
- 原生安装包

## 本地查看

```text
http://localhost:3002/login?bgssai_shell=1
http://localhost:3002/login?chat_pane=1
http://localhost:3002/          # 桌面独立，应与改前一致
```

实现：`bgssai-media-user/frontend/src/shell/detectShell.ts`。
