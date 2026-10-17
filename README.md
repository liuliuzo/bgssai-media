# bgssai-media-pc

**bgssai-media 系列是视频解决方案。本仓是 PC 端：播放器参考 VLC，同时是视频播放平台，对标 YouTube 和 YouTube Shorts。播放器要能播放主流格式文件。用户通过创作者中心发布作品，并在线观看。**

GitHub 仓库已从 `bgssai-media` 改名为 `bgssai-media-pc`。旧地址会打开本仓。

浏览器、Android、iOS 是另外三个仓库，共用本仓的用户端和管理端接口，不另建数据库：

| 仓库 | 定位 |
| --- | --- |
| bgssai-media-pc | 本仓。PC 播放器 + 平台 API |
| [bgssai-media-web](https://github.com/liuliuzo/bgssai-media-web) | Web 端平台 |
| [bgssai-media-android](https://github.com/liuliuzo/bgssai-media-android) | Android 原生播放平台 |
| [bgssai-media-ios](https://github.com/liuliuzo/bgssai-media-ios) | iOS 原生播放平台 |

当前实现边界仍见 [2026-10-06 产品定位与交付核查](docs/review/product-direction-20261006.md)。桌面播放器通过 RC 控制外部 VLC。Web 前端已迁出。Android / iOS 不再使用本仓的 Capacitor WebView 壳。

- **播放器（对照 VLC）**：按格式矩阵验收 MP4、MKV、WebM、MOV、AVI、FLV、WMV、TS、MP3、FLAC、WAV 以及 HLS。Web 与手机在线播放不继承 VLC 的解码范围。
- **视频播放平台**：正片 / 剧集连播、继续观看，以及 Shorts 竖屏流。用户在创作者中心发布作品；PC 平台壳打开 Web 用户站里的创作者中心。上游 `bgssai-short` 与 `bgssai-long` 的成片从本仓摄入接口进入目录。

## 仓库结构

| 模块 | 说明 | 本机端口 |
| --- | --- | --- |
| `bgssai-media-common` | 共享领域、MyBatis、鉴权、业务服务 | - |
| `bgssai-media-admin` | 管理端 API | 8080 |
| `bgssai-media-user` | 用户端 API、发布与在线目录 | 8081（本机并发用启动参数） |
| `bgssai-media-desktop` | Electron + libVLC 桌面播放器 | 本地 |
| `clients/desktop` | PC 平台壳，打开 Web 用户站 / 管理站 | 本地 |
| `clients/miniprogram` | 微信小程序壳 | 本地 |
| `sql/` | DDL + DML 种子 | - |

用户站和管理站的页面在 `bgssai-media-web` 的 `user/` 与 `admin/`。两端后端配置里默认 `server.port=8080`。本机同时跑两端时，用户端用 `--server.port=8081`。

## 本地准备

1. JDK 21、Maven 3.8+、Node 18+、MySQL 8
2. 初始化库：

```bash
mysql -u root -p < sql/DDL.sql
mysql -u root -p < sql/DML.sql
```

本地默认数据源（`application-local.properties`）：

- 库：`bgssai_media`
- 用户：`bgssai` / `bgssai_local`

可按需改成自己的本机账号，但不要用 `${}` 占位符。

## 启动后端

```bash
cd bgssai-media-admin
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd bgssai-media-user
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=8081
```

或打包后：

```bash
mvn -DskipTests package
java -jar bgssai-media-admin/target/bgssai-media-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -jar bgssai-media-user/target/bgssai-media-user-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8081
```

Web 前端的启动命令见 `bgssai-media-web`。用户站 http://localhost:3002 ，管理站 http://localhost:3001 。种子账号：用户 `demo` / `user123`，管理员 `admin` / `admin123`。

## 评审可验证路径

1. 用户登录后首页可见样例短剧；进入详情播放第 1 集 MP4、第 2 集 HLS
2. PC 播放器打开本地主流格式文件；Web「通用播放器」只承诺浏览器能解码的地址
3. Admin 登录后可增改剧集/分集、上下架，查看摄入日志
4. 用户端「设置」创建 MCP PAT，用 Bearer 调用 `POST /api/mcp`（见 [docs/feature/mcp.md](docs/feature/mcp.md)）
5. short → media 发布闭环：

```bash
chmod +x scripts/publish-short-smoke.sh
MEDIA_BASE_URL=http://127.0.0.1:8081 ./scripts/publish-short-smoke.sh
```

登录后在 Web 片库或 `/shorts` 查看。同一 `idempotency_key` 重放不双写。契约见 [docs/api/short-to-media-publish.md](docs/api/short-to-media-publish.md)。

## 构建检查

```bash
mvn -q -DskipTests package
cd bgssai-media-desktop && npm install && npm test
```

## 格式支持

完整矩阵见 [docs/feature/format-matrix.md](docs/feature/format-matrix.md)。

| 路径 | 支持 |
| --- | --- |
| Web / Android / iOS 在线播放 | 接口给出的 MP4 / WebM / HLS，受各端解码器限制 |
| 桌面 libVLC | mp4/mkv/webm/mov/avi/flv/ts/m3u8/mpg/wmv/3gp + 音频矩阵 |

```bash
sudo apt install -y vlc
cd bgssai-media-desktop && npm install && npm run smoke:vlc && npm start
```

PC 平台壳：

```bash
cd clients/desktop && npm install && npm start
```

壳默认打开本机 Web 登录页。微信开发者工具打开 `clients/miniprogram/`。

## 与骨架仓对齐

本仓应对齐 https://github.com/liuliuzo/bgssai-skeleton 与 https://github.com/liuliuzo/bgssai-short 的 Standards / 布局 / 鉴权约定。
