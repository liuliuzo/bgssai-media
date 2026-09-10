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

    ObjectMapper objectMapper = new ObjectMapper();
    McpRpcService rpc;

    @BeforeEach
    void setUp() {
        McpToolService tools = new McpToolService(dramaService, watchProgressService, objectMapper);
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
    void toolsListIncludesSafeReadToolsOnly() {
        ObjectNode req = objectMapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", 2);
        req.put("method", "tools/list");
        ObjectNode result = rpc.handle(req, 2L);
        String names = result.get("result").get("tools").toString();
        assertTrue(names.contains("list_published_dramas"));
        assertTrue(names.contains("get_drama"));
        assertTrue(names.contains("list_continue_watching"));
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
}
