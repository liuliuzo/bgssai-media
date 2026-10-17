package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.AnalyticsService;
import com.bgssai.media.common.service.CreatorVideoService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator")
@NeedAop(roles = {RoleCodes.USER})
public class CreatorVideoController {

    private final CreatorVideoService creatorVideoService;
    private final AnalyticsService analyticsService;

    public CreatorVideoController(CreatorVideoService creatorVideoService,
                                  AnalyticsService analyticsService) {
        this.creatorVideoService = creatorVideoService;
        this.analyticsService = analyticsService;
    }

    @PostMapping(value = "/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "cover", required = false) MultipartFile cover) throws IOException {
        byte[] coverBytes = (cover == null || cover.isEmpty()) ? null : cover.getBytes();
        return ApiResponse.ok(creatorVideoService.upload(
                AuthContext.get().getId(),
                title,
                description,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                coverBytes));
    }

    @GetMapping("/videos/mine")
    public ApiResponse<List<Map<String, Object>>> mine() {
        return ApiResponse.ok(creatorVideoService.listMine(AuthContext.get().getId()));
    }

    @GetMapping("/videos/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(creatorVideoService.detail(AuthContext.get().getId(), id));
    }

    @PatchMapping("/videos/{id}")
    public ApiResponse<Map<String, Object>> edit(@PathVariable("id") Long id,
                                                 @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(creatorVideoService.editVideo(
                AuthContext.get().getId(),
                id,
                str(body, "title"),
                str(body, "description"),
                str(body, "tags"),
                str(body, "visibility")));
    }

    @PostMapping("/videos/{id}/unpublish")
    public ApiResponse<Map<String, Object>> unpublish(@PathVariable("id") Long id) {
        return ApiResponse.ok(creatorVideoService.unpublish(AuthContext.get().getId(), id));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analyticsAggregate(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        return ApiResponse.ok(analyticsService.creatorAggregateStats(AuthContext.get().getId(), from, to));
    }

    @GetMapping("/analytics/videos/{id}")
    public ApiResponse<Map<String, Object>> analytics(@PathVariable("id") Long id,
                                                      @RequestParam(name = "from", required = false) String from,
                                                      @RequestParam(name = "to", required = false) String to) {
        return ApiResponse.ok(analyticsService.videoStats(AuthContext.get().getId(), id, from, to));
    }

    @GetMapping("/videos/{id}/playback")
    public ApiResponse<Map<String, Object>> playback(@PathVariable("id") Long id) {
        return ApiResponse.ok(creatorVideoService.playback(AuthContext.get().getId(), id));
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }

    @GetMapping("/jobs/{id}")
    public ApiResponse<Map<String, Object>> job(@PathVariable("id") Long id) {
        return ApiResponse.ok(creatorVideoService.getJob(AuthContext.get().getId(), id));
    }

    @PostMapping("/jobs/{id}/retry")
    public ApiResponse<Map<String, Object>> retry(@PathVariable("id") Long id) {
        return ApiResponse.ok(creatorVideoService.retry(AuthContext.get().getId(), id));
    }
}
