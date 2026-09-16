package com.bgssai.media.common.publish;

import com.bgssai.media.common.dto.LongDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LongDramaPublishJobTest {

    @Test
    void packagesFinishedLongWithVersionedIdempotencyKey() {
        LongDramaIngestRequest req = LongDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "3", "Long Ep1",
                "https://example.com/cover.jpg",
                "https://example.com/ep1.mp4",
                2400, "16:9", "zh-CN", List.of("long"), "reviewed");
        assertEquals(LongDramaPublishJob.SOURCE_SYSTEM, req.getSourceSystem());
        assertEquals("w1", req.getSourceWorkId());
        assertEquals("e1", req.getSourceEpisodeId());
        assertEquals("f1", req.getSourceFilmId());
        assertEquals("3", req.getSourceVersion());
        assertEquals("long:w1:e1:f1:v3", req.getIdempotencyKey());
        assertEquals(IngestStatus.READY, req.getStatus());
        assertEquals(Boolean.TRUE, req.getApproved());
        assertEquals("reviewed", req.getReviewNote());
        assertEquals("16:9", req.getAspectRatio());
    }

    @Test
    void defaultsLanguageAndAspect() {
        LongDramaIngestRequest req = LongDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "1", "T", null, "https://x/a.mp4", null, null, null, null, null);
        assertEquals("zh-CN", req.getLanguage());
        assertEquals("16:9", req.getAspectRatio());
        assertEquals(Integer.valueOf(0), req.getDurationSec());
        assertTrue(req.getTags().isEmpty());
    }

    @Test
    void missingVersionRejected() {
        BizException ex = assertThrows(BizException.class, () ->
                LongDramaPublishJob.packageFinished("w1", "e1", "f1", "", "T", null, "https://x", 1, null, null, null, null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void clientBuildsIngestRequest() {
        LongDramaPublishClient client = new LongDramaPublishClient("http://127.0.0.1:8081/");
        assertEquals("http://127.0.0.1:8081/bgssai/user/media/ingest/long-drama", client.ingestUrl());
        LongDramaIngestRequest payload = LongDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "1", "T", null, "https://example.com/a.mp4", 1, null, null, null, null);
        HttpRequest request = client.buildRequest("local-ingest-token-change-me", payload);
        assertEquals("POST", request.method());
        assertEquals("local-ingest-token-change-me",
                request.headers().firstValue(LongDramaPublishJob.TOKEN_HEADER).orElse(""));
    }

    @Test
    void payloadSerializesSnakeCase() throws Exception {
        ObjectMapper mapper = LongDramaPublishClient.snakeMapper();
        LongDramaIngestRequest req = LongDramaPublishJob.packageFinished(
                "w1", "e1", "f1", "1", "T", null, "https://example.com/a.mp4", 1, null, null, null, "ok");
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("source_work_id"));
        assertTrue(json.contains("source_version"));
        assertTrue(json.contains("idempotency_key"));
        assertTrue(json.contains("review_note"));
        assertFalse(json.contains("sourceWorkId"));
    }
}
