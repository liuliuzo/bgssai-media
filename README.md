# bgssai-media

媒体播放器 + 短剧播放/分发平台（MVP）。

- 通用 Web 播放器（MP4 / WebM / HLS）
- 短剧首页、详情、分集播放；Short 契约目录 `/shorts`
- Admin 剧集/分集 CRUD、上下架、摄入日志（不接 Chat）
- bgssai-short 服务端发布摄入 API（共享契约 + packager）
- MCP 连接器（对照 blog）：用户端 PAT + `/api/mcp` 只读工具；Admin 连接器说明页

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

Bot 壳内窄栏：给用户端 URL 加 `bgssai_shell=1` 或 `chat_pane=1`（见 [docs/feature/MOBILE-IN-BOT-SHELL.md](docs/feature/MOBILE-IN-BOT-SHELL.md)）。无参数时保持桌面独立布局。Admin 与桌面播放器不接入。

未登录默认进入登录页。用户端支持邮箱/手机 OTP（真实投递通道，未配置则失败）；Chat 第三方登录为 PREP，不签发会话。管理员仅密码登录，不接 Chat。用户登录页与页脚、管理端页脚外链至官网 5 条法务页（双语标签，**需法务审阅**，不展示自造证照编号）；见 [docs/feature/legal-fan-out.md](docs/feature/legal-fan-out.md)。

## 评审可验证路径

1. 用户登录后首页可见样例短剧；进入详情播放第 1 集 MP4、第 2 集 HLS
2. 「通用播放器」打开公开 URL 或本地 MP4/WebM 文件
3. Admin 登录后可增改剧集/分集、上下架，查看摄入日志；「MCP 连接器」页可见五款 AI 客户端说明
4. 用户端「设置」创建 MCP PAT，用 Bearer 调用 `POST /api/mcp`（见 [docs/feature/mcp.md](docs/feature/mcp.md)）
5. 用户登录页与登录后页脚可见 5 条官网法务双语外链；Admin 登录页与页脚仅外链、无“登录即同意”。均标明需法务审阅，无证照编号
6. short → media 发布闭环（推荐契约）：

```bash
# 用户端 8081
chmod +x scripts/publish-short-smoke.sh
MEDIA_BASE_URL=http://127.0.0.1:8081 ./scripts/publish-short-smoke.sh
```

登录用户端后打开 `/shorts` 播放 READY 条目。契约见 [docs/contracts/short-to-media-publish.md](docs/contracts/short-to-media-publish.md)。
MCP API 见 [docs/api/mcp.md](docs/api/mcp.md)。short 仓实现说明见 [docs/contracts/bgssai-short-implement.md](docs/contracts/bgssai-short-implement.md)。

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


## 与骨架仓对齐

本仓应对齐 https://github.com/liuliuzo/bgssai-skeleton 与 https://github.com/liuliuzo/bgssai-short 的 Standards / 布局 / 鉴权约定。
若 Cloud Agent 无权读取上述私有仓，请先授予 GitHub App 访问后再同步。
