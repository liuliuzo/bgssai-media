package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.service.CommunityService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@NeedAop(roles = {RoleCodes.USER})
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping("/channel")
    public ApiResponse<MediaUserChannel> upsertChannel(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(communityService.updateMine(
                AuthContext.get().getId(),
                str(body, "display_name"),
                str(body, "bio"),
                str(body, "handle")));
    }

    @GetMapping("/channel/mine")
    public ApiResponse<MediaUserChannel> mine() {
        return ApiResponse.ok(communityService.ensureMine(AuthContext.get().getId()));
    }

    @GetMapping("/channel/{id}")
    public ApiResponse<MediaUserChannel> channel(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.getPublic(id));
    }

    @PostMapping("/channel/{id}/follow")
    public ApiResponse<Map<String, Object>> follow(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.follow(AuthContext.get().getId(), id));
    }

    @DeleteMapping("/channel/{id}/follow")
    public ApiResponse<Map<String, Object>> unfollow(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.unfollow(AuthContext.get().getId(), id));
    }

    @GetMapping("/following")
    public ApiResponse<List<MediaUserChannel>> following() {
        return ApiResponse.ok(communityService.listFollowing(AuthContext.get().getId()));
    }

    @GetMapping("/videos")
    public ApiResponse<List<MediaCreatorVideo>> videos() {
        return ApiResponse.ok(communityService.listReadyVideos());
    }

    @GetMapping("/videos/{id}")
    public ApiResponse<MediaCreatorVideo> video(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.getReadyVideo(id));
    }

    @PostMapping("/videos/{id}/comments")
    public ApiResponse<MediaVideoComment> comment(@PathVariable("id") Long id,
                                                  @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(communityService.addComment(
                AuthContext.get().getId(), id, str(body, "content")));
    }

    @GetMapping("/videos/{id}/comments")
    public ApiResponse<List<MediaVideoComment>> comments(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.listComments(id));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable("id") Long id) {
        communityService.deleteComment(AuthContext.get().getId(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/videos/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.like(AuthContext.get().getId(), id));
    }

    @DeleteMapping("/videos/{id}/like")
    public ApiResponse<Map<String, Object>> unlike(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.unlike(AuthContext.get().getId(), id));
    }

    @GetMapping("/recommend")
    public ApiResponse<Map<String, Object>> recommend() {
        return ApiResponse.ok(communityService.recommend(AuthContext.get().getId()));
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
