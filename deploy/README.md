# 部署说明（deploy）

`bgssai-media` 是 Java 21 + Spring Boot 4 双端（`bgssai-media-user` / `bgssai-media-admin`），
以可执行 jar 运行（`java -jar`），不使用 Docker。两端共享 MySQL 库 `bgssai_media`。
前端在各模块下的 `frontend/`（Vite 开发端口 user 3002 / admin 3001）。
桌面播放器 `bgssai-media-desktop` 与六端壳 `clients/*` **不走**本服务部署。

Jenkins 走共享库 `bgssaiDeployEnd`。dev / prod 都是 **目标机自建**：目标机拉本仓 `develop`，
就地构建，再 `remote-deploy.sh` 原子替换 → 重启 → 健康检查 → 失败回滚。

**OPS-02 阻塞：** `MEDIA_USER_HOST` / `MEDIA_ADMIN_HOST` 仍为空。中央仓暂不铺
`dev|prod-media-deploy|stop`。域名未定时不要在本仓发明域名。

## 一、主机

不要在本仓写死 IP。目标机只来自 Jenkins 凭据 **Secret file** `bgssai-<env>-hosts`
（模板：`bgssai-workflows/jenkins/credentials/bgssai-hosts.{dev,prod}.env`）。

| 端 | 主机键 | 端口键（若清单提供） |
| --- | --- | --- |
| 用户端 | `MEDIA_USER_HOST` | `MEDIA_USER_PORT`（缺省 8080） |
| 管理端 | `MEDIA_ADMIN_HOST` | `MEDIA_ADMIN_PORT`（清单注释默认 **8081**） |

SSH：`bgssai-<env>-ssh`。源码：`bgssai-github`。改清单后重新上传 Secret file。
部署与关停只作用于 `bgssai-media-*` systemd 单元。OBS 等中间件 endpoint 只写
`application-*.properties` 字面量，不进 Admin / 库表。

## 二、端口

| 用途 | 端口 | 来源 |
| --- | --- | --- |
| 两端 `application.properties` | **8080** | `server.port=8080` |
| Jenkins `PRODUCT.appPort` | **8080** | 根 `Jenkinsfile`；健康协议 `http` |
| 清单里管理端注释 | **8081** | `MEDIA_ADMIN_PORT=8081` |
| 本机同时跑两端 | user 用 `--server.port=8081` | `AGENTS.md` / 根 `README.md` |
| 健康检查 | `GET /bgssai/health/readiness` | 两端 `HealthController` |
| 本机前端 Vite | admin 3001 / user 3002 | 各 `frontend` 的 Vite 配置 |

线上以主机清单端口 + systemd `--server.port` 为准。

## 三、环境与 profile

dev / prod **都拉 `develop`**，只换 profile 对应的 properties。

| Jenkins 环境 | Git 分支 | Spring profile | 配置文件 |
| --- | --- | --- | --- |
| 本机 | 任意工作树 | `local` | `application-local.properties` |
| `dev` | `develop` | `dev` | `application-dev.properties` |
| `prod` | `develop` | `prod` | `application-prod.properties` |
| （少用）`test` | Git Flow 的 `release`（若无则回落 develop） | `test` | `application-test.properties` |

环境差异写死字面量，**禁止 `${}`**。`application-secrets.properties` 已停用。
启动命令：`--spring.profiles.active=<env>`。JWT / 数据源 / OBS 等不要把真实值写进本 README。

业务第三方凭证由 Admin 维护并落库，同时在 `sql/DML.sql` 留种子。

## 四、启动

Jenkins 是唯一部署入口。环境由 Job 名决定（内部名 `dev-media-deploy` / `prod-media-deploy`），
可选 `target=both|user|admin`。关停：`jenkins/Jenkinsfile.stop`。

首次部署前在对应 MySQL 执行 `sql/DDL.sql` 与 `sql/DML.sql`（以及需要的 `sql/patch_*.sql`）。
就绪探针失败会触发 jar 回滚。

目标机工具链：git + JDK 21 + Maven + Node。一次性准备见中央仓
`jenkins/install/provision-build-host.sh`。

根 `Jenkinsfile` 声明 `frontendScript: build:deploy`。**当前树**前端在
`bgssai-media-user/frontend` 与 `bgssai-media-admin/frontend`，脚本名是 `build`，没有 `build:deploy`；
Maven 模块在仓库根下扁平存放，不是骨架仓 `<repo>-<end>/<repo>-<end>-react`。
共享库 dual 布局按骨架路径找模块。主机到位后若构建失败，先对路径与脚本名。

手动备查（本机）：

```bash
mvn -DskipTests package
java -jar bgssai-media-admin/target/bgssai-media-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
java -jar bgssai-media-user/target/bgssai-media-user-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

本机并发请给其中一端加 `--server.port=8081`。线上由 `remote-deploy.sh` 生成单元：
`java -jar app.jar --spring.profiles.active=<env> --server.port=<port>`。

摄入存储：`bgssai.media.storage.mode` 空白 = 未配置（摄入失败）；`REFERENCE` = URL 透传；
OBS 需要 properties 里的 endpoint / bucket / AK/SK 字面量（中间件参数，不进 Admin）。

## 五、回滚

Java 通道回滚是换回上一个可用 jar 并重启。

- 目标机自建失败：远端未被改动。
- 健康检查失败：`remote-deploy.sh` 已自动回滚到上一 jar。
- 不自动重跑流水线。
- SQL 补丁不随 jar 回滚。Electron / 六端安装包不在本回滚范围内。
