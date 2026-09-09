# bgssai-media

媒体播放器 + 短剧播放/分发平台（MVP）。

- 通用 Web 播放器（MP4 / WebM / HLS）
- 短剧首页、详情、分集播放
- Admin 剧集/分集 CRUD、上下架、摄入日志
- bgssai-short 服务端发布摄入 API

产品愿景见 [PRODUCT_VISION.md](PRODUCT_VISION.md)。编码规范见 [docs/BGSSAI-Standards.md](docs/BGSSAI-Standards.md)。

## 仓库结构

| 模块 | 说明 | 本机端口 |
| --- | --- | --- |
| `bgssai-media-common` | 共享领域、MyBatis、鉴权、业务服务 | - |
| `bgssai-media-admin` | 管理端后端 | 8080 |
| `bgssai-media-admin/frontend` | 管理端前端 | 3001 |
| `bgssai-media-user` | 用户端后端 | 8081（本机并发用启动参数） |
| `bgssai-media-user/frontend` | 用户端前端 | 3002 |
| `bgssai-media-desktop` | Electron + libVLC 桌面播放器 | 本地 |
| `sql/` | DDL + DML 种子 | - |

两端后端配置里默认 `server.port=8080`（与产品线一致，分环境分开部署）。本机同时跑两端时，用户端用 `--server.port=8081`。

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
# 管理端 8080
cd bgssai-media-admin
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 用户端 8081（另开终端）
cd bgssai-media-user
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=8081
```

或打包后：

```bash
mvn -DskipTests package
java -jar bgssai-media-admin/target/bgssai-media-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -jar bgssai-media-user/target/bgssai-media-user-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8081
```

## 启动前端

```bash
cd bgssai-media-admin/frontend && npm install && npm run dev
cd bgssai-media-user/frontend && npm install && npm run dev
```

- Admin：http://localhost:3001 账号 `admin` / `admin123`
- User：http://localhost:3002 账号 `demo` / `user123`

未登录默认进入登录页。用户端另支持邮箱/手机 OTP stub（固定验证码 `123456`）。

## 评审可验证路径

1. 用户登录后首页可见样例短剧；进入详情播放第 1 集 MP4、第 2 集 HLS
2. 「通用播放器」打开公开 URL 或本地 MP4/WebM 文件
3. Admin 登录后可增改剧集/分集、上下架，查看摄入日志
4. short 发布摄入（示例）：

```bash
curl -s -X POST http://127.0.0.1:8080/api/ingest/short/publish \
  -H 'Content-Type: application/json' \
  -H 'X-Ingest-Token: local-ingest-token-change-me' \
  -d '{"external_ref":"short-demo-ref-002","title":"摄入测试","status":"published","episodes":[{"ep_no":1,"title":"EP1","media_url":"https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4","status":"published"}]}'
```

契约见 [docs/api/short-publish.md](docs/api/short-publish.md)。

## 构建检查

```bash
mvn -q -DskipTests package
(cd bgssai-media-admin/frontend && npm install && npm run build)
(cd bgssai-media-user/frontend && npm install && npm run build)
```

## 格式支持（MVP）

完整矩阵见 [docs/feature/format-matrix.md](docs/feature/format-matrix.md)。

| 路径 | 支持 |
| --- | --- |
| Web 短剧端 | MP4 / WebM / HLS |
| 桌面 libVLC | mp4/mkv/webm/mov/avi/flv/ts/m3u8/mpg/wmv/3gp + 全音频矩阵 |

```bash
# 桌面播放器
sudo apt install -y vlc
cd bgssai-media-desktop && npm install && npm run smoke:vlc && npm start
```
