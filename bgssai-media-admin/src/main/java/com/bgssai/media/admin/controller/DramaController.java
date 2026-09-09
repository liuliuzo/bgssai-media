package com.bgssai.media.admin.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.service.DramaService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.common.web.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/drama")
@NeedAop(roles = {RoleCodes.PLATFORM_ADMIN})
public class DramaController {

    private final DramaService dramaService;

    public DramaController(DramaService dramaService) {
        this.dramaService = dramaService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<MediaDrama>> page(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "10") int pageSize) {
        return ApiResponse.ok(dramaService.page(status, keyword, pageNum, pageSize));
    }

    @GetMapping("/detail")
    public ApiResponse<Map<String, Object>> detail(@RequestParam("id") Long id) {
        return ApiResponse.ok(dramaService.detail(id, false));
    }

    @PostMapping("/create")
    public ApiResponse<MediaDrama> create(@RequestBody MediaDrama body) {
        return ApiResponse.ok(dramaService.create(body));
    }

    @PostMapping("/update")
    public ApiResponse<MediaDrama> update(@RequestBody MediaDrama body) {
        return ApiResponse.ok(dramaService.update(body.getId(), body));
    }

    @PostMapping("/publish")
    public ApiResponse<MediaDrama> publish(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        boolean published = Boolean.parseBoolean(String.valueOf(body.getOrDefault("published", true)));
        return ApiResponse.ok(dramaService.publish(id, published));
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        dramaService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/episode/create")
    public ApiResponse<MediaEpisode> createEpisode(@RequestBody MediaEpisode body) {
        return ApiResponse.ok(dramaService.createEpisode(body));
    }

    @PostMapping("/episode/update")
    public ApiResponse<MediaEpisode> updateEpisode(@RequestBody MediaEpisode body) {
        return ApiResponse.ok(dramaService.updateEpisode(body.getId(), body));
    }

    @PostMapping("/episode/delete")
    public ApiResponse<Void> deleteEpisode(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        dramaService.deleteEpisode(id);
        return ApiResponse.ok(null);
    }
}
