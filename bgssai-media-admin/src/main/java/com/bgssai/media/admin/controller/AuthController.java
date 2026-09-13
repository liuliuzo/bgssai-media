package com.bgssai.media.admin.controller;

import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.AuthService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin auth is password-only (DML seed). Chat / user OAuth is never accepted here.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.loginByPassword(
                body.get("username"), body.get("password"), RoleCodes.PLATFORM_ADMIN));
    }

    @GetMapping("/policy")
    public ApiResponse<Map<String, Object>> policy() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", RoleCodes.PLATFORM_ADMIN);
        result.put("password_login", true);
        result.put("chat_oauth", false);
        result.put("user_oauth", false);
        result.put("message", "admin does not accept Chat or user third-party login");
        return ApiResponse.ok(result);
    }
}
