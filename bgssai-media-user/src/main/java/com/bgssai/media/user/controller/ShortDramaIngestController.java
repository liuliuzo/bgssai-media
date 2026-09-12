package com.bgssai.media.user.controller;

import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.service.ShortDramaContractService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Shared publish contract with bgssai-short.
 * Auth: X-Bgssai-Ingest-Token (also accepts Authorization Bearer / legacy X-Ingest-Token).
 */
@RestController
public class ShortDramaIngestController {

    private final ShortDramaContractService shortDramaContractService;

    public ShortDramaIngestController(ShortDramaContractService shortDramaContractService) {
        this.shortDramaContractService = shortDramaContractService;
    }

    @PostMapping("/bgssai/user/media/ingest/short-drama")
    public ApiResponse<Map<String, Object>> ingest(
            @RequestHeader(value = "X-Bgssai-Ingest-Token", required = false) String bgssaiToken,
            @RequestHeader(value = "X-Ingest-Token", required = false) String legacyToken,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            @RequestBody ShortDramaIngestRequest body) {
        String token = firstNonBlank(bgssaiToken, legacyToken, bearer(authorization));
        shortDramaContractService.assertToken(token);
        if (body != null
                && (body.getIdempotencyKey() == null || body.getIdempotencyKey().isBlank())
                && idempotencyHeader != null
                && !idempotencyHeader.isBlank()) {
            body.setIdempotencyKey(idempotencyHeader.trim());
        }
        Map<String, Object> result = shortDramaContractService.ingest(body);
        if (IngestStatus.FAILED.equals(result.get("status"))) {
            String message = result.get("message") == null
                    ? "ingest failed"
                    : String.valueOf(result.get("message"));
            return ApiResponse.fail(422, message, result);
        }
        return ApiResponse.ok(result);
    }

    private static String bearer(String authorization) {
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
