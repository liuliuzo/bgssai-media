package com.bgssai.media.user.controller;

import com.bgssai.media.common.oauth.OAuthAuthorizeResult;
import com.bgssai.media.common.service.AuthService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GetBgssaiAuthUrlController {

    private final AuthService authService;

    public GetBgssaiAuthUrlController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/auth/oauth/bgssai/auth-url")
    public ApiResponse<Map<String, Object>> authUrl() {
        OAuthAuthorizeResult result = authService.buildOauthAuthorize("CHAT");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authorize_url", result.getAuthorizeUrl());
        map.put("state", result.getState());
        return ApiResponse.ok(map);
    }
}
