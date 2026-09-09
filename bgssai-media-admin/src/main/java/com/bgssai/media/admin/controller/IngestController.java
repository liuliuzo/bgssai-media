package com.bgssai.media.admin.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.service.IngestService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.common.web.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/short/publish")
    public ApiResponse<Map<String, Object>> shortPublish(
            @RequestHeader(value = "X-Bgssai-Ingest-Token", required = false) String bgssaiToken,
            @RequestHeader(value = "X-Ingest-Token", required = false) String legacyToken,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String token = bgssaiToken;
        if (token == null || token.isBlank()) token = legacyToken;
        if ((token == null || token.isBlank()) && authorization != null
                && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7).trim();
        }
        ingestService.assertToken(token);
        return ApiResponse.ok(ingestService.publishFromShort(body));
    }

    @GetMapping("/logs")
    @NeedAop(roles = {RoleCodes.PLATFORM_ADMIN})
    public ApiResponse<PageResult<MediaIngestLog>> logs(
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(ingestService.pageLogs(pageNum, pageSize));
    }
}
