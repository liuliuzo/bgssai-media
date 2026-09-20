package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.CreatorVideoService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public CreatorVideoController(CreatorVideoService creatorVideoService) {
        this.creatorVideoService = creatorVideoService;
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

    @GetMapping("/videos/{id}/playback")
    public ApiResponse<Map<String, Object>> playback(@PathVariable("id") Long id) {
        return ApiResponse.ok(creatorVideoService.playback(AuthContext.get().getId(), id));
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
