package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.ingest.MediaStorageGate;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShortDramaContractServiceTest {

    @Mock MediaDramaMapper mediaDramaMapper;
    @Mock MediaEpisodeMapper mediaEpisodeMapper;
    @Mock MediaIngestLogMapper mediaIngestLogMapper;
    @Mock IngestService ingestService;

    ShortDramaContractService service;
    MediaStorageGate referenceGate;
    MediaStorageGate blankGate;
    final AtomicReference<MediaIngestLog> ingest = new AtomicReference<>();
    final AtomicReference<MediaDrama> drama = new AtomicReference<>();
    final AtomicReference<MediaEpisode> episode = new AtomicReference<>();
    final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        referenceGate = new MediaStorageGate("REFERENCE", "", "", "", "");
        blankGate = new MediaStorageGate("", "", "", "", "");
        service = newService(referenceGate);
        stubMappers();
    }

    private ShortDramaContractService newService(MediaStorageGate gate) {
        return new ShortDramaContractService(
                mediaDramaMapper, mediaEpisodeMapper, mediaIngestLogMapper,
                new ObjectMapper(), ingestService, gate);
    }

    private void stubMappers() {
        when(mediaIngestLogMapper.selectByIdempotencyKey(anyString())).thenAnswer(inv -> {
            MediaIngestLog cur = ingest.get();
            return cur != null && inv.getArgument(0).equals(cur.getIdempotencyKey()) ? cur : null;
        });
        when(mediaIngestLogMapper.selectByMediaId(anyString())).thenAnswer(inv -> {
            MediaIngestLog cur = ingest.get();
            return cur != null && inv.getArgument(0).equals(cur.getMediaId()) ? cur : null;
        });
        when(mediaDramaMapper.selectByExample(any())).thenAnswer(inv ->
                drama.get() == null ? List.of() : List.of(drama.get()));
        when(mediaDramaMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaDrama d = inv.getArgument(0);
            d.setId(ids.getAndIncrement());
            drama.set(copyDrama(d));
            return 1;
        });
        when(mediaDramaMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaDrama patch = inv.getArgument(0);
            MediaDrama cur = drama.get();
            if (patch.getTitle() != null) cur.setTitle(patch.getTitle());
            if (patch.getCoverUrl() != null) cur.setCoverUrl(patch.getCoverUrl());
            if (patch.getStatus() != null) cur.setStatus(patch.getStatus());
            drama.set(cur);
            return 1;
        });
        when(mediaDramaMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv -> drama.get());
        when(mediaEpisodeMapper.selectByExample(any())).thenAnswer(inv ->
                episode.get() == null ? List.of() : List.of(episode.get()));
        when(mediaEpisodeMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaEpisode e = inv.getArgument(0);
            e.setId(ids.getAndIncrement());
            episode.set(copyEp(e));
            return 1;
        });
        when(mediaEpisodeMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaEpisode patch = inv.getArgument(0);
            MediaEpisode cur = episode.get();
            if (patch.getTitle() != null) cur.setTitle(patch.getTitle());
            if (patch.getMediaUrl() != null) cur.setMediaUrl(patch.getMediaUrl());
            if (patch.getDurationSec() != null) cur.setDurationSec(patch.getDurationSec());
            if (patch.getStatus() != null) cur.setStatus(patch.getStatus());
            episode.set(cur);
            return 1;
        });
        when(mediaEpisodeMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv -> episode.get());
        when(mediaIngestLogMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaIngestLog row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            ingest.set(copyLog(row));
            return 1;
        });
        when(mediaIngestLogMapper.updateByIdempotencyKey(any())).thenAnswer(inv -> {
            ingest.set(copyLog(inv.getArgument(0)));
            return 1;
        });
    }

    @Test
    void happyPathMarksReadyNotPublished() {
        Map<String, Object> result = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertTrue(String.valueOf(result.get("media_id")).startsWith("m_ep_"));
        assertEquals("https://cdn.example/a.mp4", result.get("play_url"));
        assertEquals(IngestStatus.READY, result.get("status"));
        assertEquals(Boolean.TRUE, result.get("playable"));
        assertEquals(Boolean.FALSE, result.get("replayed"));
        assertEquals("short:w1:e1:f1", result.get("idempotency_key"));
        assertEquals(IngestStatus.READY, ingest.get().getStatus());
    }

    @Test
    void unconfiguredStorageFailsWithoutPlayUrl() {
        service = newService(blankGate);
        Map<String, Object> result = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(IngestStatus.FAILED, result.get("status"));
        assertEquals(Boolean.FALSE, result.get("playable"));
        assertNull(result.get("play_url"));
        assertNull(result.get("media_id"));
        assertNull(ingest.get().getPlayUrl());
        assertEquals(IngestStatus.FAILED, ingest.get().getStatus());
        assertTrue(String.valueOf(result.get("message")).contains("storage not configured"));
        verify(mediaDramaMapper, never()).insertSelective(any());
    }

    @Test
    void missingAssetFailsWithoutPlayUrl() {
        ShortDramaIngestRequest req = sample("short:w1:e1:f2", "", "Ep1");
        Map<String, Object> result = service.ingest(req);
        assertEquals(IngestStatus.FAILED, result.get("status"));
        assertNull(result.get("play_url"));
        assertTrue(String.valueOf(result.get("message")).contains("asset missing"));
        assertFalse(IngestStatus.isPlayable(String.valueOf(result.get("status"))));
    }

    @Test
    void replayReturnsExistingCatalogWithoutSecondInsert() {
        Map<String, Object> r1 = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(Boolean.FALSE, r1.get("replayed"));
        Map<String, Object> r2 = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/b.mp4", "Ep1b"));
        assertEquals(r1.get("media_id"), r2.get("media_id"));
        assertEquals(r1.get("ingest_log_id"), r2.get("ingest_log_id"));
        assertEquals("https://cdn.example/a.mp4", r2.get("play_url"));
        assertEquals(IngestStatus.READY, r2.get("status"));
        assertEquals(Boolean.TRUE, r2.get("playable"));
        assertEquals(Boolean.TRUE, r2.get("replayed"));
        assertEquals("short:w1:e1:f1", r2.get("idempotency_key"));
        assertEquals("Ep1", drama.get().getTitle());
        verify(mediaIngestLogMapper, times(1)).insertSelective(any());
        verify(mediaDramaMapper, times(1)).insertSelective(any());
        verify(mediaEpisodeMapper, times(1)).insertSelective(any());
        verify(mediaIngestLogMapper, never()).updateByIdempotencyKey(any());
    }

    @Test
    void acceptsReadyApprovedPack() {
        ShortDramaIngestRequest req = sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1");
        req.setStatus(IngestStatus.READY);
        req.setApproved(Boolean.TRUE);
        Map<String, Object> result = service.ingest(req);
        assertEquals(IngestStatus.READY, result.get("status"));
        assertEquals(Boolean.TRUE, result.get("playable"));
        assertTrue(String.valueOf(result.get("media_id")).startsWith("m_ep_"));
    }

    @Test
    void rejectsUnapprovedPack() {
        ShortDramaIngestRequest req = sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1");
        req.setApproved(Boolean.FALSE);
        BizException ex = assertThrows(BizException.class, () -> service.ingest(req));
        assertEquals(422, ex.getCode());
        verify(mediaIngestLogMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsDraftPackStatus() {
        ShortDramaIngestRequest req = sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1");
        req.setStatus("DRAFT");
        BizException ex = assertThrows(BizException.class, () -> service.ingest(req));
        assertEquals(422, ex.getCode());
    }

    @Test
    void failedThenReadyUpgradesSameKey() {
        service = newService(blankGate);
        Map<String, Object> failed = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(IngestStatus.FAILED, failed.get("status"));
        service = newService(referenceGate);
        Map<String, Object> ready = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(IngestStatus.READY, ready.get("status"));
        assertEquals(Boolean.FALSE, ready.get("replayed"));
        assertEquals(failed.get("ingest_log_id"), ready.get("ingest_log_id"));
        assertTrue(String.valueOf(ready.get("media_id")).startsWith("m_ep_"));
        verify(mediaIngestLogMapper, times(1)).insertSelective(any());
        verify(mediaIngestLogMapper, times(1)).updateByIdempotencyKey(any());
    }

    @Test
    void rejectsMismatchedIdempotencyKey() {
        ShortDramaIngestRequest req = sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1");
        req.setIdempotencyKey("short:other:e1:f1");
        BizException ex = assertThrows(BizException.class, () -> service.ingest(req));
        assertEquals(400, ex.getCode());
    }

    @Test
    void duplicateKeyRaceReturnsExistingReadyCatalog() {
        MediaIngestLog winner = new MediaIngestLog();
        winner.setId(99L);
        winner.setIdempotencyKey("short:w1:e1:f1");
        winner.setMediaId("m_ep_99");
        winner.setPlayUrl("https://cdn.example/a.mp4");
        winner.setStatus(IngestStatus.READY);
        when(mediaIngestLogMapper.selectByIdempotencyKey(eq("short:w1:e1:f1")))
                .thenReturn(null)
                .thenReturn(winner);
        when(mediaIngestLogMapper.insertSelective(any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_ingest_idempotency"));
        Map<String, Object> raced = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertEquals("m_ep_99", raced.get("media_id"));
        assertEquals("https://cdn.example/a.mp4", raced.get("play_url"));
        assertEquals(Boolean.TRUE, raced.get("replayed"));
        assertEquals(IngestStatus.READY, raced.get("status"));
        assertEquals("short:w1:e1:f1", raced.get("idempotency_key"));
    }

    @Test
    void detailHidesPlayUrlWhenFailed() {
        service = newService(blankGate);
        Map<String, Object> failed = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(IngestStatus.FAILED, failed.get("status"));
        // Force a media_id onto the failed log to prove detail still refuses playable claim.
        MediaIngestLog log = ingest.get();
        log.setMediaId("m_ep_fake");
        log.setPlayUrl("https://cdn.example/should-not-expose.mp4");
        ingest.set(log);
        Map<String, Object> detail = service.detail("m_ep_fake");
        assertEquals(IngestStatus.FAILED, detail.get("status"));
        assertEquals(Boolean.FALSE, detail.get("playable"));
        assertNull(detail.get("play_url"));
    }

    @Test
    void badVideoSchemeRejected() {
        ShortDramaIngestRequest req = sample("short:w1:e1:f1", "ftp://x", "Ep1");
        BizException ex = assertThrows(BizException.class, () -> service.ingest(req));
        assertEquals(400, ex.getCode());
    }

    @Test
    void assertTokenDelegates() {
        doThrow(new BizException(401, "invalid ingest token")).when(ingestService).assertToken("wrong");
        assertThrows(BizException.class, () -> service.assertToken("wrong"));
        service.assertToken("ok");
        verify(ingestService).assertToken("ok");
    }

    private static ShortDramaIngestRequest sample(String key, String url, String title) {
        String[] parts = key.split(":");
        ShortDramaIngestRequest req = new ShortDramaIngestRequest();
        req.setSourceSystem("bgssai-short");
        req.setSourceWorkId(parts.length > 1 ? parts[1] : "w1");
        req.setSourceEpisodeId(parts.length > 2 ? parts[2] : "e1");
        req.setSourceFilmId(parts.length > 3 ? parts[3] : "f1");
        req.setTitle(title);
        req.setVideoUrl(url);
        req.setIdempotencyKey(key);
        req.setDurationSec(10);
        req.setStatus(IngestStatus.READY);
        req.setApproved(Boolean.TRUE);
        return req;
    }

    private static MediaDrama copyDrama(MediaDrama d) {
        MediaDrama n = new MediaDrama();
        n.setId(d.getId()); n.setTitle(d.getTitle()); n.setCoverUrl(d.getCoverUrl());
        n.setExternalRef(d.getExternalRef()); n.setSource(d.getSource()); n.setStatus(d.getStatus());
        return n;
    }

    private static MediaEpisode copyEp(MediaEpisode e) {
        MediaEpisode n = new MediaEpisode();
        n.setId(e.getId()); n.setDramaId(e.getDramaId()); n.setEpNo(e.getEpNo());
        n.setTitle(e.getTitle()); n.setMediaUrl(e.getMediaUrl()); n.setDurationSec(e.getDurationSec());
        n.setStorageKey(e.getStorageKey()); n.setStatus(e.getStatus());
        return n;
    }

    private static MediaIngestLog copyLog(MediaIngestLog r) {
        MediaIngestLog n = new MediaIngestLog();
        n.setId(r.getId()); n.setExternalRef(r.getExternalRef()); n.setDramaId(r.getDramaId());
        n.setIdempotencyKey(r.getIdempotencyKey()); n.setMediaId(r.getMediaId());
        n.setPlayUrl(r.getPlayUrl()); n.setStatus(r.getStatus()); n.setPayloadJson(r.getPayloadJson());
        n.setMessage(r.getMessage());
        return n;
    }
}
