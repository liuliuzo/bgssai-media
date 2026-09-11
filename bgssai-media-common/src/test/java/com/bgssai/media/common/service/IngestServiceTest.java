package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.ingest.MediaStorageGate;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngestServiceTest {

    @Mock MediaDramaMapper mediaDramaMapper;
    @Mock MediaEpisodeMapper mediaEpisodeMapper;
    @Mock MediaIngestLogMapper mediaIngestLogMapper;

    final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void stubLogInsert() {
        when(mediaIngestLogMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaIngestLog log = inv.getArgument(0);
            log.setId(ids.getAndIncrement());
            return 1;
        });
    }

    private IngestService service(MediaStorageGate gate) {
        return new IngestService(
                mediaDramaMapper, mediaEpisodeMapper, mediaIngestLogMapper,
                new ObjectMapper(), "test-token", gate);
    }

    @Test
    void unconfiguredStorageFailsClosed() {
        IngestService svc = service(new MediaStorageGate("", "", "", "", ""));
        Map<String, Object> payload = basePayload("https://cdn.example/a.mp4");
        Map<String, Object> result = svc.publishFromShort(payload);
        assertEquals(IngestStatus.FAILED, result.get("status"));
        assertEquals(Boolean.FALSE, result.get("playable"));
        assertNull(result.get("drama_id"));
        verify(mediaDramaMapper, never()).insertSelective(any());
        ArgumentCaptor<MediaIngestLog> captor = ArgumentCaptor.forClass(MediaIngestLog.class);
        verify(mediaIngestLogMapper).insertSelective(captor.capture());
        assertEquals(IngestStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void missingEpisodeAssetFailsClosed() {
        IngestService svc = service(new MediaStorageGate("REFERENCE", "", "", "", ""));
        Map<String, Object> payload = new HashMap<>();
        payload.put("external_ref", "short-1");
        payload.put("title", "T");
        payload.put("episodes", List.of(Map.of("ep_no", 1, "title", "EP1")));
        Map<String, Object> result = svc.publishFromShort(payload);
        assertEquals(IngestStatus.FAILED, result.get("status"));
        assertTrue(String.valueOf(result.get("message")).contains("asset missing"));
        verify(mediaDramaMapper, never()).insertSelective(any());
    }

    @Test
    void configuredStorageWithAssetMarksReady() {
        IngestService svc = service(new MediaStorageGate("REFERENCE", "", "", "", ""));
        when(mediaDramaMapper.selectByExample(any())).thenReturn(List.of());
        when(mediaDramaMapper.insertSelective(any())).thenAnswer(inv -> {
            inv.getArgument(0, com.bgssai.media.common.domain.MediaDrama.class).setId(10L);
            return 1;
        });
        when(mediaEpisodeMapper.selectByExample(any())).thenReturn(List.of());
        when(mediaEpisodeMapper.insertSelective(any())).thenReturn(1);

        Map<String, Object> result = svc.publishFromShort(basePayload("https://cdn.example/a.mp4"));
        assertEquals(IngestStatus.READY, result.get("status"));
        assertEquals(Boolean.TRUE, result.get("playable"));
        assertEquals(10L, result.get("drama_id"));
    }

    private static Map<String, Object> basePayload(String mediaUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("external_ref", "short-1");
        payload.put("title", "Demo");
        payload.put("episodes", List.of(Map.of(
                "ep_no", 1,
                "title", "EP1",
                "media_url", mediaUrl,
                "status", "published")));
        return payload;
    }
}
