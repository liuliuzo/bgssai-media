package com.bgssai.media.user.controller;

import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/bgssai/health/readiness")
    public ApiResponse<Map<String, String>> readiness() {
        return ApiResponse.ok(Map.of("status", "UP", "app", "bgssai-media-user"));
    }
}
