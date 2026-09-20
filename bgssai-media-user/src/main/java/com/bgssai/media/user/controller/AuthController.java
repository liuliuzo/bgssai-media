package com.bgssai.media.user.controller;

import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.oauth.OAuthAuthorizeResult;
import com.bgssai.media.common.service.AuthService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.user.auth.ChatOauthService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ChatOauthService chatOauthService;

    public AuthController(AuthService authService, ChatOauthService chatOauthService) {
        this.authService = authService;
        this.chatOauthService = chatOauthService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.loginByPassword(
                body.get("username"), body.get("password"), RoleCodes.USER));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.registerUser(
                body.get("username"), body.get("password"), body.get("email"), body.get("phone")));
    }

    @PostMapping("/otp/email/send")
    public ApiResponse<Map<String, Object>> sendEmail(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.sendEmailOtp(body.get("email")));
    }

    @PostMapping("/otp/phone/send")
    public ApiResponse<Map<String, Object>> sendPhone(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.sendPhoneOtp(body.get("phone")));
    }

    @PostMapping("/otp/email/login")
    public ApiResponse<Map<String, Object>> emailLogin(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.loginByEmailOtp(
                body.get("email"), body.get("code"), RoleCodes.USER));
    }

    @PostMapping("/otp/phone/login")
    public ApiResponse<Map<String, Object>> phoneLogin(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.loginByPhoneOtp(
                body.get("phone"), body.get("code"), RoleCodes.USER));
    }

    @PostMapping("/oauth/login")
    public ApiResponse<Map<String, Object>> oauthLogin(@RequestBody Map<String, String> body) {
        if (body.get("code") != null && !body.get("code").isBlank()) {
            return ApiResponse.ok(authService.completeOauth(
                    body.get("provider"), body.get("code"), body.get("state"), RoleCodes.USER));
        }
        return ApiResponse.ok(toMap(authService.buildOauthAuthorize(body.get("provider"))));
    }

    /**
     * GET /auth/oauth/channels — 凭证已配齐的第三方登录渠道（WECHAT / DOUYIN / BAIDU / ALIPAY / CHAT）。
     * 登录页据此决定画哪几个入口。没有这个接口时，五个入口无条件常驻，点下去才报「未配置」。
     */
    @GetMapping("/oauth/channels")
    public ApiResponse<java.util.List<String>> oauthChannels() {
        java.util.List<String> open = new java.util.ArrayList<>(authService.configuredCnProviders());
        if (chatOauthService.liveReady()) {
            open.add("CHAT");
        }
        return ApiResponse.ok(open);
    }

    @PostMapping("/oauth/authorize")
    public ApiResponse<Map<String, Object>> oauthAuthorize(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(toMap(authService.buildOauthAuthorize(body.get("provider"))));
    }

    @PostMapping("/oauth/callback")
    public ApiResponse<Map<String, Object>> oauthCallback(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.completeOauth(
                body.get("provider"), body.get("code"), body.get("state"), RoleCodes.USER));
    }

    @GetMapping("/oauth/{channel}/callback")
    public RedirectView oauthBrowserCallback(
            @PathVariable("channel") String channel,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state) {
        String provider = "bgssai".equalsIgnoreCase(channel) ? "CHAT" : channel.toUpperCase();
        return new RedirectView("/login?provider=" + provider
                + "&code=" + (code == null ? "" : code)
                + "&state=" + (state == null ? "" : state));
    }

    private static Map<String, Object> toMap(OAuthAuthorizeResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authorize_url", result.getAuthorizeUrl());
        map.put("state", result.getState());
        return map;
    }

    @GetMapping("/chat/prepare")
    public ApiResponse<Map<String, Object>> chatPrepare() {
        return ApiResponse.ok(chatOauthService.prepare());
    }

    @PostMapping("/chat/callback")
    public ApiResponse<Map<String, Object>> chatCallback(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(chatOauthService.complete(
                body == null ? null : body.get("code"),
                body == null ? null : body.get("state")));
    }
}
