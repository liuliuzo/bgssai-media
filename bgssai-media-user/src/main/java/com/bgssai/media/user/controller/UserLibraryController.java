package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.IngestLibraryService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Logged-in browse of ingested short and long assets. */
@RestController
@RequestMapping("/api/library")
@NeedAop(roles = {RoleCodes.USER})
public class UserLibraryController {

    private final IngestLibraryService ingestLibraryService;

    public UserLibraryController(IngestLibraryService ingestLibraryService) {
        this.ingestLibraryService = ingestLibraryService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> library(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(ingestLibraryService.browse(q, kind, page, pageSize));
    }
}
