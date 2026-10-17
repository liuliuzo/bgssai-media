# PC 真实播放验证 · 2026-10-17

范围：REQ-MEDIA-PC-PRODUCT-006/007/008，以及 PRODUCT-001 发布后由其他用户发现作品的 PC 入口。独立工作分支 `codex/media-real-desktop-playback-20261017`；不覆盖其他 Agent 正在审查的 OPC 需求/测试汇总。

## 已执行

Windows x64、Node 24、Electron 33.4.11，未安装系统 VLC。测试使用仓库自有样本与本机 HTTP 服务，不访问线上、真实用户数据或外部发布渠道。

- `npm test`：48 项通过。实际调用打包依赖的 FFprobe/FFmpeg，将 22 个已有样本分别生成非空 MP4，再次探测确认有真实时长、AAC 音轨。包含 MP4、MKV、MOV、AVI、FLV、WMV、TS、WebM、MPEG、3GP、HLS，以及 HEVC、AV1 与音频样本。没有样本的 APE 不标通过。
- 同一测试覆盖本机 HTTP 平台视频、临时文件回收、引擎缺失、损坏/不存在的文件、真实双音轨与内嵌字幕选择、WebVTT 输出、停止后迟到准备结果不能覆盖状态、旧会话事件不能覆盖当前视频。
- `npm run smoke:builtin`：运行实际隔离的 Electron renderer/preload 与视频元素，验证 MKV 转码后真实彩色帧像素与播放时间推进、暂停/恢复、定位、音量、倍速、停止移除媒体源、平台作品导航。截图为 [desktop-real-playback-20261017.png](desktop-real-playback-20261017.png)。平台列表使用受控夹具，不能当作线上跨用户联调证明。
- Windows 目录包：`npm run build -- --config.win.signAndEditExecutable=false` 成功。使用生成的 `BGSSAI Media Player.exe` 内的 Node 运行环境，从 `app.asar` 加载本次模块、从 `app.asar.unpacked` 解析两个真实引擎，成功转换 MKV，验证打包未遗漏依赖。
- `git diff --check` 通过；需求→HTML 原型→设计先于源码落盘。没有表结构、实体、Mapper、API 响应或部署配置变更。

## 发现并修复

- 原内置路径仅构造转码参数和虚构音轨/字幕，从未执行解码或连接画面，却标记播放中；本次使用实际媒体与 renderer 事件。
- 根目录 HLS 样本引用不存在的同级 `sample0.ts`，现指向实际的 `hls/sample0.ts`。Windows FFprobe 使用正斜杠媒体路径，避免丢失相对分段的基目录。
- 独立交叉审查复现旧准备任务等待文件检查时，新任务启动，旧任务会误删新任务目录；清理前再次检查准备代次，定向竞态回归通过。停止/退出等待已终止子进程的 close 事件后再清理，避免 Windows 文件句柄尚未释放。

## 未完成的环境验收

- 常规 `npm run build` 在本机获取 Windows 资源/签名辅助工具时，解压其 macOS 符号链接失败（系统未授予创建符号链接权限）。本次明确只验证禁用该资源编辑步骤的未签名目录构建，不宣称签名安装包交付。
- 未执行 Windows 安装/升级、macOS/Linux 构建、系统 VLC 实际播放、硬件音频设备试听、完整 HDR/杜比/图形字幕与所有编码组合验收；既有共享 JSON 的 `NOT_VERIFIED` 保持，不把有限样本升级为全矩阵通过。
- 内置解码先完整转换再播放，长视频有等待时间和磁盘需求。没有改变平台作品状态、上传转码后台、服务端观看历史或 Shorts 遥测契约。
- 未部署，dev/prod 继续部署 develop 并使用各自 properties；等待用户在 GitHub 合并。
