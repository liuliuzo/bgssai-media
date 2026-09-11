package com.bgssai.media.common.publish;

import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShortDramaPublishJobTest {

    @Test
    void packagesFinishedShortWithIdempotencyKey() {
        ShortDramaIngestRequest req = ShortDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "Demo Episode",
                "https://example.com/cover.jpg",
                "https://example.com/ep1.mp4",
                15, "9:16", "zh-CN", List.of("demo"));
        assertEquals(ShortDramaPublishJob.SOURCE_SYSTEM, req.getSourceSystem());
        assertEquals("w1", req.getSourceWorkId());
        assertEquals("e1", req.getSourceEpisodeId());
        assertEquals("f1", req.getSourceFilmId());
        assertEquals("Demo Episode", req.getTitle());
        assertEquals("https://example.com/ep1.mp4", req.getVideoUrl());
        assertEquals("short:w1:e1:f1", req.getIdempotencyKey());
        assertEquals(Integer.valueOf(15), req.getDurationSec());
        assertEquals(List.of("demo"), req.getTags());
    }

    @Test
    void defaultsLanguageAndAspect() {
        ShortDramaIngestRequest req = ShortDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "T", null, "https://x/a.mp4", null, null, null, null);
        assertEquals("zh-CN", req.getLanguage());
        assertEquals("9:16", req.getAspectRatio());
        assertEquals(Integer.valueOf(0), req.getDurationSec());
        assertTrue(req.getTags().isEmpty());
        assertNull(req.getCoverUrl());
    }

    @Test
    void missingWorkIdRejected() {
        BizException ex = assertThrows(BizException.class, () ->
                ShortDramaPublishJob.packageFinished("", "e1", "f1", "T", null, "https://x", 1, null, null, null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void clientBuildsIngestRequestWithoutEmbeddingSecrets() {
        ShortDramaPublishClient client = new ShortDramaPublishClient("http://127.0.0.1:8081/");
        assertEquals("http://127.0.0.1:8081/bgssai/user/media/ingest/short-drama", client.ingestUrl());
        ShortDramaIngestRequest payload = ShortDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "T", null, "https://example.com/a.mp4", 1, null, null, null);
        HttpRequest request = client.buildRequest("local-ingest-token-change-me", payload);
        assertEquals("POST", request.method());
        assertEquals("local-ingest-token-change-me",
                request.headers().firstValue(ShortDramaPublishJob.TOKEN_HEADER).orElse(""));
        assertFalse(request.headers().map().containsKey("X-Client-Secret"));
    }

    @Test
    void payloadSerializesSnakeCase() throws Exception {
        ObjectMapper mapper = ShortDramaPublishClient.snakeMapper();
        ShortDramaIngestRequest req = ShortDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "T", null, "https://example.com/a.mp4", 1, null, null, null);
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("source_work_id"));
        assertTrue(json.contains("idempotency_key"));
        assertTrue(json.contains("video_url"));
        assertFalse(json.contains("sourceWorkId"));
    }

    @Test
    void blankTokenRejected() {
        ShortDramaPublishClient client = new ShortDramaPublishClient("http://127.0.0.1:8081");
        ShortDramaIngestRequest payload = ShortDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "T", null, "https://example.com/a.mp4", 1, null, null, null);
        BizException ex = assertThrows(BizException.class, () -> client.buildRequest("  ", payload));
        assertEquals(401, ex.getCode());
    }
}
