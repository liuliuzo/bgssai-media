# clients/mobile — BGSSAI Media 移动壳（单 App）

**一个** Capacitor 工程同时产出 iOS 与 Android。启动闸门提供两个登录入口（用户 / 管理）。  
与桌面 Electron、微信小程序同一约定：全六端均为单 App 双入口（Boss 直聘模式）。

**禁止** 拆成 `bgssai-media-user` / `bgssai-media-admin` 两套安装包。

## 依赖

- Node 18+
- Android Studio / Xcode（按目标平台）
- 本地 user（3002）与 admin（3001）前端，或已部署 URL

## 配置

编辑 `src/config.js`：

```js
window.BGSSAI_MEDIA_SHELL = {
  userUrl: 'http://127.0.0.1:3002/login',
  adminUrl: 'http://127.0.0.1:3001/login',
};
```

真机调试请改成可访问的局域网或 HTTPS 域名。

## 运行

```bash
npm install
npm run sync
npm run add:android   # 首次
npm run add:ios       # 首次（需 macOS）
npm run open:android
# 或 npm run open:ios
```

原生工程目录 `android/`、`ios/` 由 Capacitor 生成，默认不提交（见 `.gitignore`）；CI/本机执行 `cap add` + `cap sync` 即可。

## 验收口径（本阶段）

- 模拟器/开发构建能打开启动页并看到两个入口
- 点「用户登录」进入 user 登录页；点「管理登录」进入 admin 登录页
- 仍是同一个 App ID：`com.bgssai.media`
