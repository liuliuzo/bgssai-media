package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.web.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpRpcServiceTest {

    @Mock DramaService dramaService;
    @Mock WatchProgressService watchProgressService;
    @Mock CreatorVideoService creatorVideoService;
    @Mock CommunityService communityService;

    ObjectMapper objectMapper = new ObjectMapper();
    McpRpcService rpc;

    @BeforeEach
    void setUp() {
        McpToolService tools = new McpToolService(
                dramaService, watchProgressService, creatorVideoService, communityService, objectMapper);
        rpc = new McpRpcService(tools, objectMapper);
    }

    @Test
    void initializeExposesServerAndToolsCapability() {
        ObjectNode req = objectMapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", 1);
        req.put("method", "initialize");
        ObjectNode result = rpc.handle(req, 2L);
        assertEquals("2.0", result.get("jsonrpc").asText());
        assertTrue(result.has("result"));
        assertEquals(McpRpcService.PROTOCOL_VERSION, result.get("result").get("protocolVersion").asText());
        assertEquals("bgssai-media", result.get("result").get("serverInfo").get("name").asText());
    }

    @Test
    void toolsListIncludesCatalogCreatorAndCommunityTools() {
        ObjectNode req = objectMapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", 2);
        req.put("method", "tools/list");
        ObjectNode result = rpc.handle(req, 2L);
        String names = result.get("result").get("tools").toString();
        assertTrue(names.contains("list_published_dramas"));
        assertTrue(names.contains("get_drama"));
        assertTrue(names.contains("list_continue_watching"));
        assertTrue(names.contains("list_my_videos"));
        assertTrue(names.contains("create_video_draft"));
        assertTrue(names.contains("update_video_draft"));
        assertTrue(names.contains("get_upload_task_status"));
        assertTrue(names.contains("create_community_comment"));
        assertTrue(names.contains("toggle_community_like"));
        assertTrue(names.contains("ping"));
        assertFalse(names.contains("delete"));
        assertFalse(names.contains("ingest"));
    }

    @Test
    void toolsCallListPublishedDramas() throws Exception {
        MediaDrama drama = new MediaDrama();
        drama.setId(1L);
        drama.setTitle("示例短剧");
        drama.setStatus("published");
        when(dramaService.page(eq("published"), isNull(), eq(1), eq(10)))
                .thenReturn(new PageResult<>(1, 1, 10, List.of(drama)));

        ObjectNode req = objectMapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", 3);
        req.put("method", "tools/call");
        ObjectNode params = req.putObject("params");
        params.put("name", "list_published_dramas");
        params.putObject("arguments");

        ObjectNode result = rpc.handle(req, 2L);
        assertFalse(result.get("result").get("isError").asBoolean());
        String text = result.get("result").get("content").get(0).get("text").asText();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(text, Map.class);
        assertEquals(1, ((Number) payload.get("total")).intValue());
    }

    @Test
    void toolsCallCreatorDraftAndCommunityActionsWithUserIsolation() throws Exception {
        when(creatorVideoService.createDraft(eq(88L), eq("新创作者稿件"), eq("描述"), eq("sci-fi"), isNull()))
                .thenReturn(Map.of("id", 500L, "user_id", 88L, "title", "新创作者稿件", "status", "DRAFT"));

        ObjectNode createReq = objectMapper.createObjectNode();
        createReq.put("jsonrpc", "2.0");
        createReq.put("id", 4);
        createReq.put("method", "tools/call");
        ObjectNode createParams = createReq.putObject("params");
        createParams.put("name", "create_video_draft");
        createParams.putObject("arguments")
                .put("title", "新创作者稿件")
                .put("description", "描述")
                .put("category", "sci-fi");

        ObjectNode createRes = rpc.handle(createReq, 88L);
        assertFalse(createRes.get("result").get("isError").asBoolean());
        @SuppressWarnings("unchecked")
        Map<String, Object> draftPayload = objectMapper.readValue(
                createRes.get("result").get("content").get(0).get("text").asText(), Map.class);
        assertEquals(500, ((Number) draftPayload.get("id")).intValue());
        assertEquals("DRAFT", draftPayload.get("status"));

        when(communityService.resolveContentVideoId(isNull(), eq("m_ep_701")))
                .thenReturn(CommunityService.toEpisodeVideoId(701L));
        when(communityService.toggleLike(eq(88L), eq(CommunityService.toEpisodeVideoId(701L))))
                .thenReturn(Map.of("video_id", CommunityService.toEpisodeVideoId(701L),
                        "content_id", "m_ep_701", "liked", true, "like_count", 1));

        ObjectNode likeReq = objectMapper.createObjectNode();
        likeReq.put("jsonrpc", "2.0");
        likeReq.put("id", 5);
        likeReq.put("method", "tools/call");
        ObjectNode likeParams = likeReq.putObject("params");
        likeParams.put("name", "toggle_community_like");
        likeParams.putObject("arguments").put("media_id", "m_ep_701");

        ObjectNode likeRes = rpc.handle(likeReq, 88L);
        assertFalse(likeRes.get("result").get("isError").asBoolean());
        @SuppressWarnings("unchecked")
        Map<String, Object> likePayload = objectMapper.readValue(
                likeRes.get("result").get("content").get(0).get("text").asText(), Map.class);
        assertEquals("m_ep_701", likePayload.get("content_id"));
        assertEquals(Boolean.TRUE, likePayload.get("liked"));
    }
}
