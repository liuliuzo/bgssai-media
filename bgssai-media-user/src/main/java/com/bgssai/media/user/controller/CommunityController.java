package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.service.AnalyticsService;
import com.bgssai.media.common.service.CommunityService;
import com.bgssai.media.common.service.ReportService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@NeedAop(roles = {RoleCodes.USER})
public class CommunityController {

    private final CommunityService communityService;
    private final ReportService reportService;
    private final AnalyticsService analyticsService;
    private final String sharePublicBase;

    public CommunityController(CommunityService communityService,
                               ReportService reportService,
                               AnalyticsService analyticsService,
                               @Value("${bgssai.media.share.public-base:https://media.bgssai.com}") String sharePublicBase) {
        this.communityService = communityService;
        this.reportService = reportService;
        this.analyticsService = analyticsService;
        this.sharePublicBase = sharePublicBase;
    }

    @PostMapping("/channel")
    public ApiResponse<MediaUserChannel> upsertChannel(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(communityService.updateMine(
                AuthContext.get().getId(),
                str(body, "display_name"),
                str(body, "bio"),
                str(body, "handle"),
                str(body, "avatar_url")));
    }

    @GetMapping("/channel/mine")
    public ApiResponse<MediaUserChannel> mine() {
        return ApiResponse.ok(communityService.ensureMine(AuthContext.get().getId()));
    }

    @GetMapping("/channel/{id}")
    public ApiResponse<MediaUserChannel> channel(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.getPublic(id));
    }

    @GetMapping("/channel/{id}/videos")
    public ApiResponse<List<Map<String, Object>>> channelVideos(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.listChannelVideos(id));
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

    @GetMapping("/feed/subscriptions")
    public ApiResponse<List<Map<String, Object>>> subscriptionFeed() {
        return ApiResponse.ok(communityService.subscriptionFeed(AuthContext.get().getId()));
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(@RequestParam(name = "q", required = false) String q) {
        return ApiResponse.ok(communityService.search(q));
    }

    @GetMapping("/videos")
    public ApiResponse<List<MediaCreatorVideo>> videos() {
        return ApiResponse.ok(communityService.listReadyVideos());
    }

    @GetMapping("/videos/{id}")
    public ApiResponse<MediaCreatorVideo> video(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.getReadyVideo(id));
    }

    @GetMapping("/videos/{id}/share")
    public ApiResponse<Map<String, Object>> share(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.shareLink(id, sharePublicBase));
    }

    @PostMapping("/videos/{id}/comments")
    public ApiResponse<MediaVideoComment> comment(@PathVariable("id") Long id,
                                                  @RequestBody Map<String, Object> body) {
        Long parentId = body.get("parent_id") == null ? null : Long.valueOf(String.valueOf(body.get("parent_id")));
        return ApiResponse.ok(communityService.addComment(
                AuthContext.get().getId(), id, str(body, "content"), parentId));
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

    @PostMapping("/comments/{id}/pin")
    public ApiResponse<Map<String, Object>> pinComment(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.pinComment(AuthContext.get().getId(), id, true));
    }

    @DeleteMapping("/comments/{id}/pin")
    public ApiResponse<Map<String, Object>> unpinComment(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.pinComment(AuthContext.get().getId(), id, false));
    }

    @PostMapping("/videos/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.like(AuthContext.get().getId(), id));
    }

    @DeleteMapping("/videos/{id}/like")
    public ApiResponse<Map<String, Object>> unlike(@PathVariable("id") Long id) {
        return ApiResponse.ok(communityService.unlike(AuthContext.get().getId(), id));
    }

    @PostMapping("/videos/{id}/not-interested")
    public ApiResponse<Map<String, Object>> notInterested(@PathVariable("id") Long id,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(communityService.markNotInterested(
                AuthContext.get().getId(), id, body == null ? null : str(body, "reason")));
    }

    @PostMapping("/videos/{id}/view-event")
    public ApiResponse<Map<String, Object>> viewEvent(@PathVariable("id") Long id,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        Integer watched = null;
        Boolean completed = null;
        if (body != null) {
            if (body.get("watched_sec") != null) {
                watched = Integer.valueOf(String.valueOf(body.get("watched_sec")));
            }
            if (body.get("completed") != null) {
                completed = Boolean.valueOf(String.valueOf(body.get("completed")));
            }
        }
        return ApiResponse.ok(analyticsService.recordView(AuthContext.get().getId(), id, watched, completed));
    }

    @PostMapping("/reports")
    public ApiResponse<Map<String, Object>> report(@RequestBody Map<String, Object> body) {
        Long targetId = body.get("target_id") == null ? null : Long.valueOf(String.valueOf(body.get("target_id")));
        return ApiResponse.ok(reportService.submit(
                AuthContext.get().getId(),
                str(body, "target_type"),
                targetId,
                str(body, "reason")));
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
