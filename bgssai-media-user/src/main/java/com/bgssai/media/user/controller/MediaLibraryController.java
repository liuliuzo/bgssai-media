package com.bgssai.media.user.controller;

import com.bgssai.media.common.service.IngestLibraryService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public browse of READY short and long assets ingested into this media instance. */
@RestController
public class MediaLibraryController {

    private final IngestLibraryService ingestLibraryService;

    public MediaLibraryController(IngestLibraryService ingestLibraryService) {
        this.ingestLibraryService = ingestLibraryService;
    }

    @GetMapping("/bgssai/user/media/library")
    public ApiResponse<Map<String, Object>> library(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(ingestLibraryService.browse(q, kind, page, pageSize));
    }
}
