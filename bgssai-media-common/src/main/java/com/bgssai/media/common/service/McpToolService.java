package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP toolset for bgssai-media: catalog queries, continue-watching, creator video drafts
 * and upload task status, and community comments/likes.
 *
 * <p>All user/creator/community tools enforce strict user ownership isolation under the
 * authenticated PAT userId.
 */
@Service
public class McpToolService {

    private final DramaService dramaService;
    private final WatchProgressService watchProgressService;
    private final CreatorVideoService creatorVideoService;
    private final CommunityService communityService;
    private final ObjectMapper objectMapper;

    public McpToolService(DramaService dramaService,
                          WatchProgressService watchProgressService,
                          ObjectMapper objectMapper) {
        this(dramaService, watchProgressService, null, null, objectMapper);
    }

    @Autowired
    public McpToolService(DramaService dramaService,
                          WatchProgressService watchProgressService,
                          @Autowired(required = false) CreatorVideoService creatorVideoService,
                          @Autowired(required = false) CommunityService communityService,
                          ObjectMapper objectMapper) {
        this.dramaService = dramaService;
        this.watchProgressService = watchProgressService;
        this.creatorVideoService = creatorVideoService;
        this.communityService = communityService;
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
        tools.add(tool(
                "list_my_videos",
                "List the authenticated creator's uploaded and draft videos.",
                schema(
                        Map.of("status", prop("string", "Optional status filter (DRAFT, QUEUED, RUNNING, READY, FAILED)")),
                        List.of())));
        tools.add(tool(
                "create_video_draft",
                "Create a new creator video draft owned by the authenticated user.",
                schema(
                        Map.of(
                                "title", prop("string", "Video draft title"),
                                "description", prop("string", "Video description (optional)"),
                                "category", prop("string", "Video category or tag (optional)"),
                                "cover_url", prop("string", "Cover image URL (optional)")),
                        List.of("title"))));
        tools.add(tool(
                "update_video_draft",
                "Update metadata (title, description, category, cover_url) of a video draft owned by the authenticated user.",
                schema(
                        Map.of(
                                "video_id", prop("integer", "Video ID owned by the authenticated user"),
                                "title", prop("string", "Updated title (optional)"),
                                "description", prop("string", "Updated description (optional)"),
                                "category", prop("string", "Updated category (optional)"),
                                "cover_url", prop("string", "Updated cover URL (optional)")),
                        List.of("video_id"))));
        tools.add(tool(
                "get_upload_task_status",
                "Get upload, transcode, and review status for a creator video owned by the authenticated user.",
                schema(
                        Map.of(
                                "video_id", prop("integer", "Creator video ID (optional if job_id provided)"),
                                "job_id", prop("integer", "Transcode job ID (optional if video_id provided)")),
                        List.of())));
        tools.add(tool(
                "create_community_comment",
                "Post a comment on a published community video or drama episode as the authenticated user.",
                schema(
                        Map.of(
                                "video_id", prop("integer", "Community video ID or mapped episode video ID"),
                                "media_id", prop("string", "Unified content or episode media_id (e.g. m_ep_1)"),
                                "content", prop("string", "Comment text")),
                        List.of("content"))));
        tools.add(tool(
                "toggle_community_like",
                "Toggle like on a published community video or drama episode as the authenticated user.",
                schema(
                        Map.of(
                                "video_id", prop("integer", "Community video ID or mapped episode video ID"),
                                "media_id", prop("string", "Unified content or episode media_id (e.g. m_ep_1)")),
                        List.of())));
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
                case "list_continue_watching" -> {
                    requireUser(userId);
                    yield watchProgressService.listByUser(userId);
                }
                case "list_my_videos" -> listMyVideos(arguments, userId);
                case "create_video_draft" -> createVideoDraft(arguments, userId);
                case "update_video_draft" -> updateVideoDraft(arguments, userId);
                case "get_upload_task_status" -> getUploadTaskStatus(arguments, userId);
                case "create_community_comment" -> createCommunityComment(arguments, userId);
                case "toggle_community_like" -> toggleCommunityLike(arguments, userId);
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

    private Map<String, Object> listMyVideos(JsonNode arguments, Long userId) {
        requireUser(userId);
        requireCreatorService();
        String statusFilter = textArg(arguments, "status");
        List<Map<String, Object>> all = creatorVideoService.listMine(userId);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : all) {
            if (statusFilter == null || statusFilter.equalsIgnoreCase(String.valueOf(item.get("status")))) {
                filtered.add(item);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user_id", userId);
        out.put("total", filtered.size());
        out.put("list", filtered);
        return out;
    }

    private Map<String, Object> createVideoDraft(JsonNode arguments, Long userId) {
        requireUser(userId);
        requireCreatorService();
        String title = textArg(arguments, "title");
        String description = textArg(arguments, "description");
        String category = textArg(arguments, "category");
        String coverUrl = textArg(arguments, "cover_url");
        return creatorVideoService.createDraft(userId, title, description, category, coverUrl);
    }

    private Map<String, Object> updateVideoDraft(JsonNode arguments, Long userId) {
        requireUser(userId);
        requireCreatorService();
        long videoId = longArg(arguments, "video_id");
        String title = textArg(arguments, "title");
        String description = textArg(arguments, "description");
        String category = textArg(arguments, "category");
        String coverUrl = textArg(arguments, "cover_url");
        return creatorVideoService.updateDraft(userId, videoId, title, description, category, coverUrl);
    }

    private Map<String, Object> getUploadTaskStatus(JsonNode arguments, Long userId) {
        requireUser(userId);
        requireCreatorService();
        Long videoId = optionalLongArg(arguments, "video_id");
        Long jobId = optionalLongArg(arguments, "job_id");
        return creatorVideoService.getUploadTaskStatus(userId, videoId, jobId);
    }

    private Map<String, Object> createCommunityComment(JsonNode arguments, Long userId) {
        requireUser(userId);
        requireCommunityService();
        Long videoId = optionalLongArg(arguments, "video_id");
        String mediaId = textArg(arguments, "media_id");
        String content = textArg(arguments, "content");
        Long resolvedVideoId = communityService.resolveContentVideoId(videoId, mediaId);
        MediaVideoComment comment = communityService.addComment(userId, resolvedVideoId, content);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("comment_id", comment.getId());
        out.put("video_id", comment.getVideoId());
        out.put("content_id", CommunityService.toContentId(comment.getVideoId()));
        out.put("user_id", comment.getUserId());
        out.put("content", comment.getContent());
        out.put("created_at", comment.getCreatedAt());
        return out;
    }

    private Map<String, Object> toggleCommunityLike(JsonNode arguments, Long userId) {
        requireUser(userId);
        requireCommunityService();
        Long videoId = optionalLongArg(arguments, "video_id");
        String mediaId = textArg(arguments, "media_id");
        Long resolvedVideoId = communityService.resolveContentVideoId(videoId, mediaId);
        return communityService.toggleLike(userId, resolvedVideoId);
    }

    private void requireCreatorService() {
        if (creatorVideoService == null) {
            throw new BizException(500, "creator video service unavailable");
        }
    }

    private void requireCommunityService() {
        if (communityService == null) {
            throw new BizException(500, "community service unavailable");
        }
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(401, "login required");
        }
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

    private Long optionalLongArg(JsonNode arguments, String field) {
        if (arguments == null || arguments.isNull() || !arguments.has(field) || arguments.get(field).isNull()) {
            return null;
        }
        JsonNode node = arguments.get(field);
        if (node.isNumber()) {
            return node.asLong();
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(400, "invalid " + field);
        }
    }
}
