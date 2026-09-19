package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.AuthUser;
import com.bgssai.media.common.auth.JwtAuthInterceptor;
import com.bgssai.media.common.auth.JwtService;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.domain.MediaSupportSession;
import com.bgssai.media.common.service.SupportService;
import com.bgssai.media.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户侧站内在线客服。创建会话允许匿名（尽量采集联系方式）；
 * 读/写己方会话凭 session_token 或登录用户归属。
 */
@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;
    private final JwtService jwtService;

    public SupportController(SupportService supportService, JwtService jwtService) {
        this.supportService = supportService;
        this.jwtService = jwtService;
    }

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        Long userId = optionalUserId(request);
        String clientKey = clientKey(request, userId);
        return ApiResponse.ok(supportService.createSession(
                userId,
                str(body, "contact_name"),
                str(body, "contact_email"),
                str(body, "contact_phone"),
                str(body, "subject"),
                str(body, "content"),
                clientKey));
    }

    @GetMapping("/sessions/mine")
    @NeedAop(roles = {RoleCodes.USER})
    public ApiResponse<List<MediaSupportSession>> mine() {
        return ApiResponse.ok(supportService.listMine(AuthContext.get().getId()));
    }

    @GetMapping("/sessions/detail")
    public ApiResponse<Map<String, Object>> detail(@RequestParam(value = "session_id", required = false) Long sessionId,
                                                   @RequestParam(value = "session_token", required = false) String sessionToken,
                                                   HttpServletRequest request) {
        Long userId = optionalUserId(request);
        return ApiResponse.ok(supportService.getForUser(sessionId, sessionToken, userId));
    }

    @PostMapping("/sessions/messages")
    public ApiResponse<Map<String, Object>> send(@RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        Long userId = optionalUserId(request);
        Long sessionId = body.get("session_id") == null ? null : Long.valueOf(String.valueOf(body.get("session_id")));
        String sessionToken = str(body, "session_token");
        return ApiResponse.ok(supportService.sendUserMessage(
                sessionId, sessionToken, userId, str(body, "content"), clientKey(request, userId)));
    }

    @PostMapping("/sessions/read")
    public ApiResponse<Map<String, Object>> markRead(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        Long userId = optionalUserId(request);
        Long sessionId = body.get("session_id") == null ? null : Long.valueOf(String.valueOf(body.get("session_id")));
        String sessionToken = str(body, "session_token");
        return ApiResponse.ok(supportService.markUserRead(sessionId, sessionToken, userId));
    }

    private Long optionalUserId(HttpServletRequest request) {
        AuthUser fromCtx = AuthContext.get();
        if (fromCtx != null && RoleCodes.USER.equals(fromCtx.getRole_code())) {
            return fromCtx.getId();
        }
        String token = request.getHeader(JwtAuthInterceptor.HEADER);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            AuthUser user = jwtService.parse(token.trim());
            if (user != null && RoleCodes.USER.equals(user.getRole_code())) {
                return user.getId();
            }
        } catch (Exception ignored) {
            // 匿名访客：忽略无效 token
        }
        return null;
    }

    private static String clientKey(HttpServletRequest request, Long userId) {
        if (userId != null) {
            return "u:" + userId;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + (request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr());
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
