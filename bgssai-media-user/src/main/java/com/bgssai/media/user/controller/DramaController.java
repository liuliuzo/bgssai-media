package com.bgssai.media.user.controller;

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
@NeedAop(roles = {RoleCodes.USER})
public class DramaController {

    private final DramaService dramaService;

    public DramaController(DramaService dramaService) {
        this.dramaService = dramaService;
    }

    @GetMapping("/feed")
    public ApiResponse<PageResult<MediaDrama>> feed(
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "12") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.ok(dramaService.page("published", keyword, pageNum, pageSize));
    }

    @GetMapping("/detail")
    public ApiResponse<Map<String, Object>> detail(@RequestParam("id") Long id) {
        return ApiResponse.ok(dramaService.detail(id, true));
    }

    @GetMapping("/episode")
    public ApiResponse<MediaEpisode> episode(@RequestParam("id") Long id) {
        MediaEpisode ep = dramaService.getEpisode(id);
        if (!"published".equals(ep.getStatus())) {
            return ApiResponse.fail(404, "episode not found");
        }
        return ApiResponse.ok(ep);
    }
}
