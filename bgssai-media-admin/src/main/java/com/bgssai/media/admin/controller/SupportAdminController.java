package com.bgssai.media.admin.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaSupportSession;
import com.bgssai.media.common.service.SupportService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.common.web.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端客服处理台：列表、详情、回复、关闭、标记已读。
 */
@RestController
@RequestMapping("/api/support")
@NeedAop(roles = {RoleCodes.PLATFORM_ADMIN})
public class SupportAdminController {

    private final SupportService supportService;

    public SupportAdminController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/sessions/page")
    public ApiResponse<PageResult<MediaSupportSession>> page(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(supportService.adminPage(status, keyword, pageNum, pageSize));
    }

    @GetMapping("/sessions/detail")
    public ApiResponse<Map<String, Object>> detail(@RequestParam("session_id") Long sessionId) {
        return ApiResponse.ok(supportService.adminDetail(sessionId));
    }

    @PostMapping("/sessions/reply")
    public ApiResponse<Map<String, Object>> reply(@RequestBody Map<String, Object> body) {
        Long sessionId = Long.valueOf(String.valueOf(body.get("session_id")));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        return ApiResponse.ok(supportService.adminReply(sessionId, AuthContext.get().getId(), content));
    }

    @PostMapping("/sessions/close")
    public ApiResponse<Map<String, Object>> close(@RequestBody Map<String, Object> body) {
        Long sessionId = Long.valueOf(String.valueOf(body.get("session_id")));
        return ApiResponse.ok(supportService.adminClose(sessionId, AuthContext.get().getId()));
    }

    @PostMapping("/sessions/read")
    public ApiResponse<Map<String, Object>> markRead(@RequestBody Map<String, Object> body) {
        Long sessionId = Long.valueOf(String.valueOf(body.get("session_id")));
        return ApiResponse.ok(supportService.adminMarkRead(sessionId));
    }
}
