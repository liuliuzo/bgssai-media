# PC 真实解码与平台作品

## 桌面内置路径

主进程解析打包的 ffmpeg-static / ffprobe-static 可执行文件（asar 解包目录）；环境覆盖仅用于引擎定位。FFprobe 以参数数组探测真实流及 duration；不存在的文件、无音视频流、解码失败均拒绝。每次媒体准备使用独立系统临时目录，通过 FFmpeg 实际生成 MP4 与可选 WebVTT，成功退出且输出存在后才能生成 playback_url。选取音轨使用实际流索引；图形字幕不提供内置选择。所有子进程无 shell，隐藏命令窗口。

VlcPlayer 内置会话包含 sessionId、playbackUrl、subtitleUrl、durationSec、seekSec、音轨/字幕和 desiredPlaying；准备时 ready=false。渲染层仅在 sessionId 改变时装载视频，用实际 play/pause/timeupdate/error/ended 事件回报主进程；旧 session 回报丢弃。seek 使用当前 MP4 的真实时间轴；更换音轨生成新会话并恢复位置。停止递增准备代次、终止旧进程，异步完成不得覆盖后续会话。暂停/音量/倍速同步到实际 HTMLVideoElement，全屏使用该元素。

临时目录按代次回收，删除失败延迟退出时重试；原始文件不删除。打包声明 asarUnpack，包含两套引擎依赖；beforePack 实际运行两个二进制的版本命令，安装脚本未执行、缺引擎或不可执行时拒绝生成缺失解码器的包。VLC 存在时保留 RC 路径；不把内置路径的样本结果写入全矩阵通过状态。

## 平台视频

桌面平台页使用 GET /api/community/videos?page_num=1&page_size=20，再经 GET /api/creator/videos/{id}/playback 获取当前可播 rendition，交给现有 platform:play。IPC API 白名单扩展到 community 只读路径；令牌仍仅在主进程。直接地址验证维持 HTTP/HTTPS，异常不伪造就绪。不新增端点、表、实体或 Mapper。
