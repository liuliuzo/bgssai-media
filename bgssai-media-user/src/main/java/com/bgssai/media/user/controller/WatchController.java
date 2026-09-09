package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaWatchProgress;
import com.bgssai.media.common.service.WatchProgressService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watch")
@NeedAop(roles = {RoleCodes.USER})
public class WatchController {

    private final WatchProgressService watchProgressService;

    public WatchController(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    @GetMapping("/continue")
    public ApiResponse<List<MediaWatchProgress>> continueWatching() {
        return ApiResponse.ok(watchProgressService.listByUser(AuthContext.get().getId()));
    }

    @PostMapping("/progress")
    public ApiResponse<MediaWatchProgress> save(@RequestBody Map<String, Object> body) {
        Long dramaId = Long.valueOf(String.valueOf(body.get("drama_id")));
        Long episodeId = Long.valueOf(String.valueOf(body.get("episode_id")));
        Integer positionSec = body.get("position_sec") == null ? 0 : Integer.valueOf(String.valueOf(body.get("position_sec")));
        return ApiResponse.ok(watchProgressService.save(
                AuthContext.get().getId(), dramaId, episodeId, positionSec));
    }
}
