package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.service.McpTokenService;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP PAT 管理（对照 blog：/api/mcp-tokens）。
 */
@RestController
@RequestMapping("/api/mcp-tokens")
@NeedAop(roles = {RoleCodes.USER})
public class McpTokenController {

    private final McpTokenService mcpTokenService;

    public McpTokenController(McpTokenService mcpTokenService) {
        this.mcpTokenService = mcpTokenService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(mcpTokenService.listForUser(AuthContext.get().getId()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String name = body == null || body.get("name") == null ? null : String.valueOf(body.get("name"));
        return ApiResponse.ok(mcpTokenService.create(AuthContext.get().getId(), name));
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<Void> revoke(@PathVariable("id") Long id) {
        mcpTokenService.revoke(AuthContext.get().getId(), id);
        return ApiResponse.ok(null);
    }
}
