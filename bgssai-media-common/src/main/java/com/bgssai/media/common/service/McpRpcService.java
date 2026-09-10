package com.bgssai.media.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * Minimal Streamable HTTP MCP JSON-RPC handler (initialize / tools / ping).
 */
@Service
public class McpRpcService {

    public static final String PROTOCOL_VERSION = "2025-03-26";

    private final McpToolService mcpToolService;
    private final ObjectMapper objectMapper;

    public McpRpcService(McpToolService mcpToolService, ObjectMapper objectMapper) {
        this.mcpToolService = mcpToolService;
        this.objectMapper = objectMapper;
    }

    public ObjectNode handle(JsonNode request, Long userId) {
        if (request == null || request.isNull()) {
            return error(null, -32600, "invalid request");
        }
        if (request.isArray()) {
            // Batch not required for MVP clients (Cursor / Claude via mcp-remote).
            return error(null, -32600, "batch requests not supported");
        }
        String method = text(request, "method");
        JsonNode id = request.get("id");
        JsonNode params = request.get("params");
        if (method == null || method.isBlank()) {
            return error(id, -32600, "method required");
        }
        // Notifications have no id and expect no response body from some transports;
        // still return an empty ack object so HTTP clients get 200 JSON.
        if ("notifications/initialized".equals(method) || "notifications/cancelled".equals(method)) {
            ObjectNode ack = objectMapper.createObjectNode();
            ack.put("jsonrpc", "2.0");
            if (id != null && !id.isNull()) {
                ack.set("id", id);
            }
            ack.putNull("result");
            return ack;
        }
        return switch (method) {
            case "initialize" -> ok(id, initializeResult());
            case "ping" -> ok(id, objectMapper.createObjectNode().put("ok", true));
            case "tools/list" -> ok(id, objectMapper.createObjectNode().set("tools", mcpToolService.listTools()));
            case "tools/call" -> {
                String name = params == null ? null : text(params, "name");
                JsonNode arguments = params == null ? null : params.get("arguments");
                yield ok(id, mcpToolService.callTool(name, arguments, userId));
            }
            default -> error(id, -32601, "method not found: " + method);
        };
    }

    private ObjectNode initializeResult() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);
        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.set("tools", objectMapper.createObjectNode());
        result.set("capabilities", capabilities);
        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "bgssai-media");
        serverInfo.put("version", "0.1.0");
        serverInfo.put("title", "Connect your AI tools to bgssai");
        result.set("serverInfo", serverInfo);
        ObjectNode instructions = objectMapper.createObjectNode();
        // Keep instructions as string per MCP initialize result.
        return result.put("instructions",
                "bgssai-media MCP: read-only short-drama catalog and continue-watching. "
                        + "Supported clients: Claude, Codex, Cursor, Grok Bot, bgssai-bot.");
    }

    private ObjectNode ok(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null && !id.isNull()) {
            response.set("id", id);
        } else {
            response.putNull("id");
        }
        response.set("result", result);
        return response;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null && !id.isNull()) {
            response.set("id", id);
        } else {
            response.putNull("id");
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
