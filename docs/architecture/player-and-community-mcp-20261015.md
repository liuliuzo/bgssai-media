# Desktop Self-Contained Decoder, Drama-Community Unification & Creator MCP (2026-10-15)

## 1. Self-Contained Desktop Playback Engine (`bgssai-media-desktop/electron/vlc-player.js`)
- Removes the hard failure when an external host VLC installation is absent.
- Automatically selects `libvlc-rc` when a host VLC binary is present and falls back seamlessly to the built-in `builtin-html5-ffmpeg` self-contained engine on clean Windows, macOS, and Linux machines.
- Supports session creation (`openMedia`), play/pause/stop, seek (`seekTo`), and audio/subtitle track switching (`selectAudioTrack`, `selectSubtitleTrack`) in both engine modes.

## 2. Short & Long Drama Community Unification (`CommunityService.java`)
- Published and approved Short/Long drama episodes (`media_drama` + `media_episode` with playable `media_ingest_log` status) are projected into community ready streams (`listReadyVideos`) and two-stage recommendations (`recommend`) without duplicating library tables.
- Uses deterministic unified content IDs (`EPISODE_VIDEO_ID_OFFSET + episodeId` and `m_ep_<episodeId>`) so comments (`addComment`, `listComments`) and likes (`like`, `unlike`, `toggleLike`) work uniformly across creator uploads and drama episodes.

## 3. Creator & Community MCP Tools (`McpToolService.java`)
- Expands `/api/mcp` with `list_my_videos`, `create_video_draft`, `update_video_draft`, `get_upload_task_status`, `create_community_comment`, and `toggle_community_like`, all scoped strictly to the authenticated PAT `user_id`.
