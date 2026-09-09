package com.bgssai.media.user.controller;

import com.bgssai.media.common.service.ShortDramaContractService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.common.web.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MediaShortsController {

    private final ShortDramaContractService shortDramaContractService;

    public MediaShortsController(ShortDramaContractService shortDramaContractService) {
        this.shortDramaContractService = shortDramaContractService;
    }

    @GetMapping("/bgssai/user/media/shorts")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        PageResult<Map<String, Object>> result = shortDramaContractService.list(q, page, pageSize);
        Map<String, Object> body = new HashMap<>();
        body.put("list", result.getList());
        body.put("total", result.getTotal());
        body.put("page", result.getPageNum());
        body.put("page_size", result.getPageSize());
        body.put("q", q);
        return ApiResponse.ok(body);
    }

    @GetMapping("/bgssai/user/media/shorts/{mediaId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String mediaId) {
        return ApiResponse.ok(shortDramaContractService.detail(mediaId));
    }
}
