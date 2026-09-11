package com.bgssai.media.user.controller;

import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.AuthService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.user.auth.ChatOauthService;
import org.springframework.web.bind.annotation.*;

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
        return ApiResponse.ok(authService.loginByOauth(body.get("provider"), RoleCodes.USER));
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
