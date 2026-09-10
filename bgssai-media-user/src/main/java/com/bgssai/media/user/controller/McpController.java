package com.bgssai.media.user.controller;

import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.service.McpRpcService;
import com.bgssai.media.common.service.McpTokenService;
import com.bgssai.media.common.web.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Streamable HTTP MCP endpoint（对照 blog：POST /api/mcp + Authorization: Bearer PAT）。
 * 无 @NeedAop：由 PAT 鉴权，不走 Jwttoken。
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpTokenService mcpTokenService;
    private final McpRpcService mcpRpcService;

    public McpController(McpTokenService mcpTokenService, McpRpcService mcpRpcService) {
        this.mcpTokenService = mcpTokenService;
        this.mcpRpcService = mcpRpcService;
    }

    @GetMapping
    public ResponseEntity<?> probe(HttpServletRequest request) {
        SysUser user = authenticate(request);
        if (user == null) {
            return unauthorized();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product", "bgssai-media");
        body.put("transport", "streamable-http");
        body.put("protocol_version", McpRpcService.PROTOCOL_VERSION);
        body.put("message", "Connect your AI tools to bgssai");
        body.put("auth", "Authorization: Bearer <mcp_pat>");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handle(@RequestBody JsonNode body, HttpServletRequest request) {
        SysUser user = authenticate(request);
        if (user == null) {
            return unauthorized();
        }
        ObjectNode rpc = mcpRpcService.handle(body, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("MCP-Protocol-Version", McpRpcService.PROTOCOL_VERSION)
                .body(rpc);
    }

    private SysUser authenticate(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            auth = request.getHeader("authorization");
        }
        return mcpTokenService.authenticate(auth);
    }

    private static ResponseEntity<ApiResponse<Void>> unauthorized() {
        return ResponseEntity.status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(401, "invalid or missing MCP token"));
    }
}
