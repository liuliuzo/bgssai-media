package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.UserLibrarySyncService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Watch history / watch-later / playlists under /api/library/* (PRODUCT-012).
 * Coexists with ingest browse on GET /api/library.
 */
@RestController
@RequestMapping("/api/library")
@NeedAop(roles = {RoleCodes.USER})
public class LibrarySyncController {

    private final UserLibrarySyncService librarySyncService;

    public LibrarySyncController(UserLibrarySyncService librarySyncService) {
        this.librarySyncService = librarySyncService;
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(librarySyncService.listHistory(AuthContext.get().getId(), limit));
    }

    @PostMapping("/history")
    public ApiResponse<Map<String, Object>> recordHistory(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(librarySyncService.recordHistory(
                AuthContext.get().getId(),
                asLong(body.get("video_id")),
                asInt(body.get("watched_sec"))));
    }

    @DeleteMapping("/history/{videoId}")
    public ApiResponse<Void> deleteHistory(@PathVariable("videoId") Long videoId) {
        librarySyncService.deleteHistoryItem(AuthContext.get().getId(), videoId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/history/pause")
    public ApiResponse<Map<String, Object>> getPause() {
        return ApiResponse.ok(librarySyncService.getHistoryPrefs(AuthContext.get().getId()));
    }

    @PutMapping("/history/pause")
    public ApiResponse<Map<String, Object>> setPause(@RequestBody Map<String, Object> body) {
        boolean paused = Boolean.TRUE.equals(body.get("paused"))
                || Boolean.TRUE.equals(body.get("pause_watch_history"))
                || "true".equalsIgnoreCase(String.valueOf(body.getOrDefault("paused", "false")));
        return ApiResponse.ok(librarySyncService.setPauseWatchHistory(AuthContext.get().getId(), paused));
    }

    @GetMapping("/watch-later")
    public ApiResponse<List<Map<String, Object>>> watchLater() {
        return ApiResponse.ok(librarySyncService.listWatchLater(AuthContext.get().getId()));
    }

    @PostMapping("/watch-later")
    public ApiResponse<Map<String, Object>> addWatchLater(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(librarySyncService.addWatchLater(
                AuthContext.get().getId(), asLong(body.get("video_id"))));
    }

    @DeleteMapping("/watch-later/{videoId}")
    public ApiResponse<Void> removeWatchLater(@PathVariable("videoId") Long videoId) {
        librarySyncService.removeWatchLater(AuthContext.get().getId(), videoId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/playlists")
    public ApiResponse<List<Map<String, Object>>> playlists() {
        return ApiResponse.ok(librarySyncService.listPlaylists(AuthContext.get().getId()));
    }

    @PostMapping("/playlists")
    public ApiResponse<Map<String, Object>> createPlaylist(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(librarySyncService.createPlaylist(
                AuthContext.get().getId(),
                str(body.get("title")),
                str(body.get("visibility"))));
    }

    @GetMapping("/playlists/{id}")
    public ApiResponse<Map<String, Object>> playlist(@PathVariable("id") Long id) {
        return ApiResponse.ok(librarySyncService.getPlaylist(AuthContext.get().getId(), id));
    }

    @PostMapping("/playlists/{id}/items")
    public ApiResponse<Map<String, Object>> addItem(@PathVariable("id") Long id,
                                                    @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(librarySyncService.addPlaylistItem(
                AuthContext.get().getId(), id, asLong(body.get("video_id"))));
    }

    @DeleteMapping("/playlists/{id}/items/{videoId}")
    public ApiResponse<Void> removeItem(@PathVariable("id") Long id,
                                        @PathVariable("videoId") Long videoId) {
        librarySyncService.removePlaylistItem(AuthContext.get().getId(), id, videoId);
        return ApiResponse.ok(null);
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

    private static Integer asInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.valueOf(String.valueOf(v));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
