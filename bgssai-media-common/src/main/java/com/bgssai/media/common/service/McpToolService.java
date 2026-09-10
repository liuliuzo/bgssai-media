package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 只读安全工具集（媒体仓 MVP）：目录检索、剧集详情、继续观看。
 * 不做写入、不做管理端操作、不回显媒体密钥。
 */
@Service
public class McpToolService {

    private final DramaService dramaService;
    private final WatchProgressService watchProgressService;
    private final ObjectMapper objectMapper;

    public McpToolService(DramaService dramaService,
                          WatchProgressService watchProgressService,
                          ObjectMapper objectMapper) {
        this.dramaService = dramaService;
        this.watchProgressService = watchProgressService;
        this.objectMapper = objectMapper;
    }

    public ArrayNode listTools() {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(tool(
                "ping",
                "Health check for the bgssai-media MCP server.",
                objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode())));
        tools.add(tool(
                "list_published_dramas",
                "List published short dramas. Optional keyword filter and pagination.",
                schema(
                        Map.of(
                                "keyword", prop("string", "Title keyword (optional)"),
                                "page_num", prop("integer", "Page number, default 1"),
                                "page_size", prop("integer", "Page size, default 10, max 50")),
                        List.of())));
        tools.add(tool(
                "get_drama",
                "Get one published drama with its published episodes (no admin drafts).",
                schema(
                        Map.of("drama_id", prop("integer", "Drama id")),
                        List.of("drama_id"))));
        tools.add(tool(
                "list_continue_watching",
                "List the authenticated user's continue-watching progress.",
                objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode())));
        return tools;
    }

    public ObjectNode callTool(String name, JsonNode arguments, Long userId) {
        try {
            Object payload = switch (name == null ? "" : name) {
                case "ping" -> Map.of(
                        "ok", true,
                        "product", "bgssai-media",
                        "message", "Connect your AI tools to bgssai");
                case "list_published_dramas" -> listPublishedDramas(arguments);
                case "get_drama" -> getDrama(arguments);
                case "list_continue_watching" -> watchProgressService.listByUser(userId);
                default -> throw new BizException(404, "unknown tool: " + name);
            };
            return textResult(objectMapper.writeValueAsString(payload), false);
        } catch (BizException ex) {
            return textResult(ex.getMessage(), true);
        } catch (Exception ex) {
            return textResult("tool failed: " + ex.getMessage(), true);
        }
    }

    private Map<String, Object> listPublishedDramas(JsonNode arguments) {
        String keyword = textArg(arguments, "keyword");
        int pageNum = intArg(arguments, "page_num", 1);
        int pageSize = Math.min(50, Math.max(1, intArg(arguments, "page_size", 10)));
        PageResult<MediaDrama> page = dramaService.page("published", keyword, pageNum, pageSize);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", page.getTotal());
        out.put("page_num", page.getPageNum());
        out.put("page_size", page.getPageSize());
        out.put("list", page.getList());
        return out;
    }

    private Map<String, Object> getDrama(JsonNode arguments) {
        long dramaId = longArg(arguments, "drama_id");
        return dramaService.detail(dramaId, true);
    }

    private ObjectNode textResult(String text, boolean isError) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "text");
        item.put("text", text);
        content.add(item);
        result.set("content", content);
        result.put("isError", isError);
        return result;
    }

    private ObjectNode tool(String name, String description, ObjectNode inputSchema) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        node.put("description", description);
        node.set("inputSchema", inputSchema);
        return node;
    }

    private ObjectNode schema(Map<String, ObjectNode> properties, List<String> required) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        for (Map.Entry<String, ObjectNode> e : properties.entrySet()) {
            props.set(e.getKey(), e.getValue());
        }
        schema.set("properties", props);
        if (!required.isEmpty()) {
            ArrayNode req = objectMapper.createArrayNode();
            required.forEach(req::add);
            schema.set("required", req);
        }
        return schema;
    }

    private ObjectNode prop(String type, String description) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        node.put("description", description);
        return node;
    }

    private String textArg(JsonNode arguments, String field) {
        if (arguments == null || arguments.isNull() || !arguments.has(field) || arguments.get(field).isNull()) {
            return null;
        }
        String value = arguments.get(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int intArg(JsonNode arguments, String field, int defaultValue) {
        if (arguments == null || arguments.isNull() || !arguments.has(field) || arguments.get(field).isNull()) {
            return defaultValue;
        }
        return arguments.get(field).asInt(defaultValue);
    }

    private long longArg(JsonNode arguments, String field) {
        if (arguments == null || arguments.isNull() || !arguments.has(field) || arguments.get(field).isNull()) {
            throw new BizException(400, field + " required");
        }
        return arguments.get(field).asLong();
    }
}
