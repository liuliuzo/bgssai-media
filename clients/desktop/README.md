# clients/desktop — 管理站入口

PC 用户界面在 `bgssai-media-desktop`。那个应用自己绘制播放器、正片、短视频和创作者中心，调用本仓用户端 API，不打开 Web 用户站。

本目录只保留管理入口，打开 `bgssai-media-web` 的管理站。

## 运行

先在 `bgssai-media-web` 启动 `admin`（3001），再启动本目录。用户播放请启动 `bgssai-media-desktop`。

```bash
cd clients/desktop
npm install
npm start
```

启动后看到说明页。点「管理入口」打开管理站。菜单「入口 → 返回双入口」可回到说明页。

```bash
BGSSAI_MEDIA_ADMIN_URL=https://media-admin.bgssai.com/login \
npm start
```

## 打包（后置）

```bash
npm run dist:linux
npm run dist:win
npm run dist:mac
```
