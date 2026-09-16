package com.bgssai.media.user.controller;

import com.bgssai.media.common.service.LongDramaContractService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.common.web.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** Public contract endpoints for long-drama catalog / play (MEDIA-02). */
@RestController
public class MediaLongsController {

    private final LongDramaContractService longDramaContractService;

    public MediaLongsController(LongDramaContractService longDramaContractService) {
        this.longDramaContractService = longDramaContractService;
    }

    @GetMapping("/bgssai/user/media/longs")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        PageResult<Map<String, Object>> result = longDramaContractService.list(q, page, pageSize);
        Map<String, Object> body = new HashMap<>();
        body.put("list", result.getList());
        body.put("total", result.getTotal());
        body.put("page", result.getPageNum());
        body.put("page_size", result.getPageSize());
        body.put("q", q);
        return ApiResponse.ok(body);
    }

    @GetMapping("/bgssai/user/media/longs/{mediaId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String mediaId) {
        return ApiResponse.ok(longDramaContractService.detail(mediaId));
    }

    @GetMapping("/bgssai/user/media/longs/works/{sourceWorkId}/episodes")
    public ApiResponse<Map<String, Object>> episodes(@PathVariable String sourceWorkId) {
        return ApiResponse.ok(longDramaContractService.listEpisodesByWork(sourceWorkId));
    }
}
