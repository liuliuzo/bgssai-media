package com.bgssai.media.admin.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.ReportService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@NeedAop(roles = {RoleCodes.PLATFORM_ADMIN})
public class ModerationAdminController {

    private final ReportService reportService;

    public ModerationAdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public ApiResponse<List<Map<String, Object>>> reports(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(reportService.listOpen(limit));
    }

    @PostMapping("/reports/{id}/resolve")
    public ApiResponse<Map<String, Object>> resolve(@PathVariable("id") Long id,
                                                    @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(reportService.resolve(
                id,
                body == null ? null : String.valueOf(body.get("status")),
                body == null || body.get("note") == null ? null : String.valueOf(body.get("note"))));
    }

    @PostMapping("/videos/{id}/moderation")
    public ApiResponse<Map<String, Object>> moderate(@PathVariable("id") Long id,
                                                     @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(reportService.moderateVideo(
                id,
                body == null || body.get("status") == null ? null : String.valueOf(body.get("status"))));
    }
}
