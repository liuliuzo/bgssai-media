package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.dto.ShortDramaIngestRequest;
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
    final AtomicReference<MediaIngestLog> ingest = new AtomicReference<>();
    final AtomicReference<MediaDrama> drama = new AtomicReference<>();
    final AtomicReference<MediaEpisode> episode = new AtomicReference<>();
    final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        service = new ShortDramaContractService(
                mediaDramaMapper, mediaEpisodeMapper, mediaIngestLogMapper, new ObjectMapper(), ingestService);

        when(mediaIngestLogMapper.selectByIdempotencyKey(anyString())).thenAnswer(inv -> {
            MediaIngestLog cur = ingest.get();
            return cur != null && inv.getArgument(0).equals(cur.getIdempotencyKey()) ? cur : null;
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
    void happyPath() {
        Map<String, Object> result = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        assertTrue(String.valueOf(result.get("media_id")).startsWith("m_ep_"));
        assertEquals("https://cdn.example/a.mp4", result.get("play_url"));
        assertEquals("PUBLISHED", result.get("status"));
    }

    @Test
    void duplicateUpdatesSameMediaId() {
        Map<String, Object> r1 = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/a.mp4", "Ep1"));
        Map<String, Object> r2 = service.ingest(sample("short:w1:e1:f1", "https://cdn.example/b.mp4", "Ep1b"));
        assertEquals(r1.get("media_id"), r2.get("media_id"));
        assertEquals("https://cdn.example/b.mp4", r2.get("play_url"));
        assertEquals("Ep1b", drama.get().getTitle());
    }

    @Test
    void badVideoRejected() {
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
        ShortDramaIngestRequest req = new ShortDramaIngestRequest();
        req.setSourceSystem("bgssai-short");
        req.setSourceWorkId("w1");
        req.setSourceEpisodeId("1");
        req.setSourceFilmId("f1");
        req.setTitle(title);
        req.setVideoUrl(url);
        req.setIdempotencyKey(key);
        req.setDurationSec(10);
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
        return n;
    }
}
