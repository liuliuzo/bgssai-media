package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.OpcExtMapper;
import com.bgssai.media.common.web.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-client watch history / watch-later / playlists (PRODUCT-012).
 * Mounted under /api/library/* alongside ingest library browse.
 */
@Service
public class UserLibrarySyncService {

    private final OpcExtMapper opcExtMapper;
    private final CommunityService communityService;
    private final MediaCreatorVideoMapper videoMapper;

    public UserLibrarySyncService(OpcExtMapper opcExtMapper,
                                  CommunityService communityService,
                                  MediaCreatorVideoMapper videoMapper) {
        this.opcExtMapper = opcExtMapper;
        this.communityService = communityService;
        this.videoMapper = videoMapper;
    }

    public Map<String, Object> getHistoryPrefs(Long userId) {
        requireUser(userId);
        Integer paused = opcExtMapper.getPauseWatchHistory(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pause_watch_history", paused != null && paused == 1);
        return out;
    }

    @Transactional
    public Map<String, Object> setPauseWatchHistory(Long userId, boolean paused) {
        requireUser(userId);
        opcExtMapper.upsertPauseWatchHistory(userId, paused ? 1 : 0);
        return getHistoryPrefs(userId);
    }

    @Transactional
    public Map<String, Object> recordHistory(Long userId, Long videoId, Integer watchedSec) {
        requireUser(userId);
        Integer paused = opcExtMapper.getPauseWatchHistory(userId);
        if (paused != null && paused == 1) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("recorded", false);
            out.put("reason", "pause_watch_history");
            return out;
        }
        MediaCreatorVideo video = communityService.getReadyVideo(videoId);
        opcExtMapper.upsertWatchHistory(
                userId,
                videoId,
                CommunityService.toContentId(videoId),
                video.getTitle(),
                watchedSec == null ? 0 : Math.max(0, watchedSec),
                1);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recorded", true);
        out.put("video_id", videoId);
        return out;
    }

    public List<Map<String, Object>> listHistory(Long userId, int limit) {
        requireUser(userId);
        List<Map<String, Object>> rows = opcExtMapper.listWatchHistory(userId, Math.min(Math.max(limit, 1), 200));
        for (Map<String, Object> row : rows) {
            Long videoId = asLong(row.get("videoId"));
            try {
                communityService.getReadyVideo(videoId);
                row.put("playable", true);
            } catch (BizException ex) {
                row.put("playable", false);
            }
            normalizeKeys(row);
        }
        return rows;
    }

    @Transactional
    public void deleteHistoryItem(Long userId, Long videoId) {
        requireUser(userId);
        opcExtMapper.deleteWatchHistoryItem(userId, videoId);
    }

    public List<Map<String, Object>> listWatchLater(Long userId) {
        requireUser(userId);
        List<Map<String, Object>> rows = opcExtMapper.listWatchLater(userId);
        for (Map<String, Object> row : rows) {
            normalizeKeys(row);
            enrichVideoMeta(row);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> addWatchLater(Long userId, Long videoId) {
        requireUser(userId);
        communityService.getReadyVideo(videoId);
        opcExtMapper.insertWatchLater(userId, videoId, 0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("video_id", videoId);
        out.put("added", true);
        return out;
    }

    @Transactional
    public void removeWatchLater(Long userId, Long videoId) {
        requireUser(userId);
        opcExtMapper.deleteWatchLater(userId, videoId);
    }

    public List<Map<String, Object>> listPlaylists(Long userId) {
        requireUser(userId);
        List<Map<String, Object>> rows = opcExtMapper.listPlaylists(userId);
        for (Map<String, Object> row : rows) {
            normalizeKeys(row);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> createPlaylist(Long userId, String title, String visibility) {
        requireUser(userId);
        if (title == null || title.isBlank()) {
            throw new BizException(400, "title required");
        }
        String vis = visibility == null || visibility.isBlank() ? "private" : visibility.trim().toLowerCase();
        if (!"private".equals(vis) && !"public".equals(vis)) {
            throw new BizException(400, "invalid visibility");
        }
        Map<String, Object> row = new HashMap<>();
        row.put("userId", userId);
        row.put("title", title.trim());
        row.put("visibility", vis);
        opcExtMapper.insertPlaylist(row);
        normalizeKeys(row);
        return row;
    }

    public Map<String, Object> getPlaylist(Long userId, Long playlistId) {
        requireUser(userId);
        Map<String, Object> pl = requirePlaylistAccess(userId, playlistId);
        List<Map<String, Object>> items = opcExtMapper.listPlaylistItems(playlistId);
        for (Map<String, Object> item : items) {
            normalizeKeys(item);
            enrichVideoMeta(item);
        }
        Map<String, Object> out = new LinkedHashMap<>(pl);
        normalizeKeys(out);
        out.put("items", items);
        return out;
    }

    @Transactional
    public Map<String, Object> addPlaylistItem(Long userId, Long playlistId, Long videoId) {
        requireUser(userId);
        requirePlaylistOwner(userId, playlistId);
        communityService.getReadyVideo(videoId);
        opcExtMapper.insertPlaylistItem(playlistId, videoId, 0);
        return getPlaylist(userId, playlistId);
    }

    @Transactional
    public Map<String, Object> reorderPlaylistItems(Long userId, Long playlistId, List<Long> videoIds) {
        requireUser(userId);
        requirePlaylistOwner(userId, playlistId);
        if (videoIds == null || videoIds.isEmpty()) {
            throw new BizException(400, "video_ids required");
        }
        int order = 0;
        for (Long videoId : videoIds) {
            if (videoId == null) {
                continue;
            }
            opcExtMapper.updatePlaylistItemOrder(playlistId, videoId, order++);
        }
        return getPlaylist(userId, playlistId);
    }

    @Transactional
    public void removePlaylistItem(Long userId, Long playlistId, Long videoId) {
        requireUser(userId);
        requirePlaylistOwner(userId, playlistId);
        opcExtMapper.deletePlaylistItem(playlistId, videoId);
    }

    private void enrichVideoMeta(Map<String, Object> row) {
        Long videoId = asLong(row.get("video_id"));
        if (videoId == null) {
            videoId = asLong(row.get("videoId"));
        }
        String title = null;
        String cover = null;
        boolean playable = false;
        if (videoId != null) {
            try {
                MediaCreatorVideo ready = communityService.getReadyVideo(videoId);
                title = ready.getTitle();
                cover = coverOf(ready);
                playable = true;
            } catch (BizException ex) {
                MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
                if (video != null) {
                    title = video.getTitle();
                    cover = coverOf(video);
                }
            }
        }
        row.put("title", title);
        row.put("cover", cover);
        row.put("playable", playable);
    }

    private static String coverOf(MediaCreatorVideo video) {
        if (video.getCoverUrl() != null && !video.getCoverUrl().isBlank()) {
            return video.getCoverUrl();
        }
        return video.getCoverPath();
    }

    private Map<String, Object> requirePlaylistAccess(Long userId, Long playlistId) {
        Map<String, Object> pl = opcExtMapper.getPlaylist(playlistId);
        if (pl == null) {
            throw new BizException(404, "playlist not found");
        }
        Long owner = asLong(pl.get("userId"));
        String vis = String.valueOf(pl.get("visibility"));
        if (!userId.equals(owner) && !"public".equalsIgnoreCase(vis)) {
            throw new BizException(403, "forbidden");
        }
        return pl;
    }

    private void requirePlaylistOwner(Long userId, Long playlistId) {
        Map<String, Object> pl = opcExtMapper.getPlaylist(playlistId);
        if (pl == null) {
            throw new BizException(404, "playlist not found");
        }
        if (!userId.equals(asLong(pl.get("userId")))) {
            throw new BizException(403, "forbidden");
        }
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(401, "login required");
        }
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(v));
    }

    private static void normalizeKeys(Map<String, Object> row) {
        rename(row, "videoId", "video_id");
        rename(row, "userId", "user_id");
        rename(row, "contentId", "content_id");
        rename(row, "watchedSec", "watched_sec");
        rename(row, "lastWatchedAt", "last_watched_at");
        rename(row, "sortOrder", "sort_order");
        rename(row, "createdAt", "created_at");
        rename(row, "updatedAt", "updated_at");
    }

    private static void rename(Map<String, Object> row, String from, String to) {
        if (row.containsKey(from)) {
            row.put(to, row.remove(from));
        }
    }
}
