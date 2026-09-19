# clients/desktop — BGSSAI Media 桌面壳（单 App）

**一个** Electron 安装包覆盖 Windows / macOS / Linux。启动即双入口闸门（用户入口 / 管理入口），对齐 Boss 直聘。

**禁止** 拆成 Admin / User 两套桌面程序。

与仓库根下 `bgssai-media-desktop`（libVLC 本地播放器）职责不同。

## 运行

```bash
cd ../../bgssai-media-user/frontend && npm run dev
cd ../../bgssai-media-admin/frontend && npm run dev

cd clients/desktop
npm install
npm start
```

启动后先见闸门页；点入口进入对应 Web 登录。菜单「入口 → 返回双入口」可切回闸门。

```bash
BGSSAI_MEDIA_USER_URL=https://media.bgssai.com/login \
BGSSAI_MEDIA_ADMIN_URL=https://media-admin.bgssai.com/login \
npm start
```

## 打包（后置）

```bash
npm run dist:linux
npm run dist:win
npm run dist:mac
```
