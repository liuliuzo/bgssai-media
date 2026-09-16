package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.dto.LongDramaIngestRequest;
import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.ingest.MediaStorageGate;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.bgssai.media.common.source.SourceRefs;
import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LongDramaContractServiceTest {

    @Mock MediaDramaMapper mediaDramaMapper;
    @Mock MediaEpisodeMapper mediaEpisodeMapper;
    @Mock MediaIngestLogMapper mediaIngestLogMapper;
    @Mock IngestService ingestService;

    LongDramaContractService longService;
    ShortDramaContractService shortService;
    MediaStorageGate referenceGate;
    MediaStorageGate blankGate;

    final Map<String, MediaIngestLog> ingestByKey = new HashMap<>();
    final Map<String, MediaIngestLog> ingestByMediaId = new HashMap<>();
    final Map<String, MediaDrama> dramaByRef = new HashMap<>();
    final Map<Long, MediaDrama> dramaById = new HashMap<>();
    final Map<String, MediaEpisode> episodeByDramaEp = new HashMap<>();
    final Map<Long, MediaEpisode> episodeById = new HashMap<>();
    final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        referenceGate = new MediaStorageGate("REFERENCE", "", "", "", "");
        blankGate = new MediaStorageGate("", "", "", "", "");
        longService = newLong(referenceGate);
        shortService = newShort(referenceGate);
        stubMappers();
    }

    private LongDramaContractService newLong(MediaStorageGate gate) {
        return new LongDramaContractService(
                mediaDramaMapper, mediaEpisodeMapper, mediaIngestLogMapper,
                snakeMapper(), ingestService, gate);
    }

    private ShortDramaContractService newShort(MediaStorageGate gate) {
        return new ShortDramaContractService(
                mediaDramaMapper, mediaEpisodeMapper, mediaIngestLogMapper,
                snakeMapper(), ingestService, gate);
    }

    private void stubMappers() {
        when(mediaIngestLogMapper.selectByIdempotencyKey(anyString())).thenAnswer(inv -> {
            MediaIngestLog cur = ingestByKey.get(inv.getArgument(0));
            return cur == null ? null : copyLog(cur);
        });
        when(mediaIngestLogMapper.selectByMediaId(anyString())).thenAnswer(inv -> {
            MediaIngestLog cur = ingestByMediaId.get(inv.getArgument(0));
            return cur == null ? null : copyLog(cur);
        });
        when(mediaDramaMapper.selectByExample(any())).thenAnswer(inv -> {
            MediaDramaExample ex = inv.getArgument(0);
            String ref = extractExternalRef(ex);
            if (ref == null) return List.of();
            MediaDrama d = dramaByRef.get(ref);
            return d == null ? List.of() : List.of(copyDrama(d));
        });
        when(mediaDramaMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaDrama d = inv.getArgument(0);
            d.setId(ids.getAndIncrement());
            MediaDrama stored = copyDrama(d);
            dramaByRef.put(stored.getExternalRef(), stored);
            dramaById.put(stored.getId(), stored);
            return 1;
        });
        when(mediaDramaMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaDrama patch = inv.getArgument(0);
            MediaDrama cur = dramaById.get(patch.getId());
            if (patch.getTitle() != null) cur.setTitle(patch.getTitle());
            if (patch.getCoverUrl() != null) cur.setCoverUrl(patch.getCoverUrl());
            if (patch.getStatus() != null) cur.setStatus(patch.getStatus());
            if (patch.getSource() != null) cur.setSource(patch.getSource());
            dramaByRef.put(cur.getExternalRef(), cur);
            return 1;
        });
        when(mediaDramaMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv -> {
            MediaDrama d = dramaById.get(inv.getArgument(0));
            return d == null ? null : copyDrama(d);
        });
        when(mediaEpisodeMapper.selectByExample(any())).thenAnswer(inv -> {
            MediaEpisodeExample ex = inv.getArgument(0);
            Long dramaId = extractDramaId(ex);
            Integer epNo = extractEpNo(ex);
            if (dramaId != null && epNo != null) {
                MediaEpisode e = episodeByDramaEp.get(dramaId + ":" + epNo);
                return e == null ? List.of() : List.of(copyEp(e));
            }
            if (dramaId != null) {
                return episodeByDramaEp.values().stream()
                        .filter(e -> dramaId.equals(e.getDramaId()))
                        .sorted(Comparator.comparing(MediaEpisode::getEpNo))
                        .map(LongDramaContractServiceTest::copyEp)
                        .collect(Collectors.toList());
            }
            return List.of();
        });
        when(mediaEpisodeMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaEpisode e = inv.getArgument(0);
            e.setId(ids.getAndIncrement());
            MediaEpisode stored = copyEp(e);
            episodeByDramaEp.put(stored.getDramaId() + ":" + stored.getEpNo(), stored);
            episodeById.put(stored.getId(), stored);
            return 1;
        });
        when(mediaEpisodeMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaEpisode patch = inv.getArgument(0);
            MediaEpisode cur = episodeById.get(patch.getId());
            if (patch.getTitle() != null) cur.setTitle(patch.getTitle());
            if (patch.getMediaUrl() != null) cur.setMediaUrl(patch.getMediaUrl());
            if (patch.getDurationSec() != null) cur.setDurationSec(patch.getDurationSec());
            if (patch.getStorageKey() != null) cur.setStorageKey(patch.getStorageKey());
            if (patch.getStatus() != null) cur.setStatus(patch.getStatus());
            episodeByDramaEp.put(cur.getDramaId() + ":" + cur.getEpNo(), cur);
            return 1;
        });
        when(mediaEpisodeMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv -> {
            MediaEpisode e = episodeById.get(inv.getArgument(0));
            return e == null ? null : copyEp(e);
        });
        when(mediaIngestLogMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaIngestLog row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            MediaIngestLog stored = copyLog(row);
            ingestByKey.put(stored.getIdempotencyKey(), stored);
            if (stored.getMediaId() != null) {
                ingestByMediaId.put(stored.getMediaId(), stored);
            }
            return 1;
        });
        when(mediaIngestLogMapper.updateByIdempotencyKey(any())).thenAnswer(inv -> {
            MediaIngestLog row = inv.getArgument(0);
            MediaIngestLog stored = copyLog(row);
            ingestByKey.put(stored.getIdempotencyKey(), stored);
            if (stored.getMediaId() != null) {
                ingestByMediaId.put(stored.getMediaId(), stored);
            } else {
                ingestByMediaId.entrySet().removeIf(e ->
                        stored.getIdempotencyKey().equals(e.getValue().getIdempotencyKey()));
            }
            return 1;
        });
    }

    @Test
    void happyPathMarksReady() {
        Map<String, Object> result = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/a.mp4", "Ep1"));
        assertTrue(String.valueOf(result.get("media_id")).startsWith("m_ep_"));
        assertEquals(IngestStatus.READY, result.get("status"));
        assertEquals(Boolean.TRUE, result.get("playable"));
        assertEquals(Boolean.FALSE, result.get("replayed"));
        assertEquals("long:w1:e1:f1:v1", result.get("idempotency_key"));
        assertEquals("1", result.get("source_version"));
        assertEquals(SourceRefs.SYSTEM_LONG, dramaByRef.get("long:work:w1").getSource());
        assertEquals("long:film:f1", episodeById.values().iterator().next().getStorageKey());
    }

    @Test
    void longAndShortSameUpstreamIdsDoNotConflict() {
        Map<String, Object> shortResult = shortService.ingest(shortSample("w1", "e1", "f1",
                "https://cdn.example/short.mp4", "Short Ep"));
        Map<String, Object> longResult = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/long.mp4", "Long Ep"));
        assertNotEquals(shortResult.get("media_id"), longResult.get("media_id"));
        assertTrue(dramaByRef.containsKey("short:work:w1"));
        assertTrue(dramaByRef.containsKey("long:work:w1"));
        assertNotEquals(dramaByRef.get("short:work:w1").getId(), dramaByRef.get("long:work:w1").getId());
        assertEquals("short:w1:e1:f1", shortResult.get("idempotency_key"));
        assertEquals("long:w1:e1:f1:v1", longResult.get("idempotency_key"));
    }

    @Test
    void crossEpisodeSortedByEpNo() {
        longService.ingest(sample("w1", "e3", "f3", "1", "https://cdn.example/e3.mp4", "Ep3"));
        longService.ingest(sample("w1", "e1", "f1", "1", "https://cdn.example/e1.mp4", "Ep1"));
        longService.ingest(sample("w1", "e2", "f2", "1", "https://cdn.example/e2.mp4", "Ep2"));
        Map<String, Object> work = longService.listEpisodesByWork("w1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> episodes = (List<Map<String, Object>>) work.get("episodes");
        assertEquals(3, episodes.size());
        assertEquals(1, episodes.get(0).get("ep_no"));
        assertEquals(2, episodes.get(1).get("ep_no"));
        assertEquals(3, episodes.get(2).get("ep_no"));
        assertEquals(Boolean.TRUE, episodes.get(0).get("playable"));
        assertEquals("https://cdn.example/e1.mp4", episodes.get(0).get("play_url"));
    }

    @Test
    void newVersionReplacesSameEpisodePlayUrlKeepsMediaId() {
        Map<String, Object> v1 = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/v1.mp4", "Ep1 v1"));
        String mediaId = String.valueOf(v1.get("media_id"));
        Map<String, Object> v2 = longService.ingest(sample("w1", "e1", "f2", "2",
                "https://cdn.example/v2.mp4", "Ep1 v2"));
        assertEquals(mediaId, v2.get("media_id"));
        assertEquals("https://cdn.example/v2.mp4", v2.get("play_url"));
        assertEquals(Boolean.FALSE, v2.get("replayed"));
        assertEquals(2, ingestByKey.size());
        assertEquals(1, episodeByDramaEp.size());
        MediaEpisode ep = episodeByDramaEp.values().iterator().next();
        assertEquals("https://cdn.example/v2.mp4", ep.getMediaUrl());
        assertEquals("long:film:f2", ep.getStorageKey());
        Map<String, Object> detail = longService.detail(mediaId);
        assertEquals("https://cdn.example/v2.mp4", detail.get("play_url"));
        assertEquals("2", detail.get("source_version"));
    }

    @Test
    void duplicateSubmissionReplaysWithoutSecondInsert() {
        Map<String, Object> r1 = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/a.mp4", "Ep1"));
        Map<String, Object> r2 = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/b.mp4", "Ep1b"));
        assertEquals(r1.get("media_id"), r2.get("media_id"));
        assertEquals(r1.get("ingest_log_id"), r2.get("ingest_log_id"));
        assertEquals("https://cdn.example/a.mp4", r2.get("play_url"));
        assertEquals(Boolean.TRUE, r2.get("replayed"));
        verify(mediaIngestLogMapper, times(1)).insertSelective(any());
        verify(mediaIngestLogMapper, never()).updateByIdempotencyKey(any());
    }

    @Test
    void failedThenReadyUpgradesSameKey() {
        longService = newLong(blankGate);
        Map<String, Object> failed = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(IngestStatus.FAILED, failed.get("status"));
        longService = newLong(referenceGate);
        Map<String, Object> ready = longService.ingest(sample("w1", "e1", "f1", "1",
                "https://cdn.example/a.mp4", "Ep1"));
        assertEquals(IngestStatus.READY, ready.get("status"));
        assertEquals(Boolean.FALSE, ready.get("replayed"));
        assertEquals(failed.get("ingest_log_id"), ready.get("ingest_log_id"));
        assertTrue(String.valueOf(ready.get("media_id")).startsWith("m_ep_"));
        verify(mediaIngestLogMapper, times(1)).insertSelective(any());
        verify(mediaIngestLogMapper, times(1)).updateByIdempotencyKey(any());
    }

    @Test
    void rejectsUnapprovedPack() {
        LongDramaIngestRequest req = sample("w1", "e1", "f1", "1", "https://cdn.example/a.mp4", "Ep1");
        req.setApproved(Boolean.FALSE);
        BizException ex = assertThrows(BizException.class, () -> longService.ingest(req));
        assertEquals(422, ex.getCode());
    }

    @Test
    void rejectsMismatchedIdempotencyKey() {
        LongDramaIngestRequest req = sample("w1", "e1", "f1", "1", "https://cdn.example/a.mp4", "Ep1");
        req.setIdempotencyKey("long:w1:e1:f1:v9");
        BizException ex = assertThrows(BizException.class, () -> longService.ingest(req));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shortOldContractStillWorksAfterSourceRefs() {
        Map<String, Object> result = shortService.ingest(shortSample("w9", "e9", "f9",
                "https://cdn.example/s.mp4", "Legacy"));
        assertEquals(IngestStatus.READY, result.get("status"));
        assertEquals("short:w9:e9:f9", result.get("idempotency_key"));
        assertEquals("short:work:w9", dramaByRef.keySet().iterator().next());
        assertEquals("film:f9", episodeById.values().iterator().next().getStorageKey());
    }

    private static LongDramaIngestRequest sample(
            String work, String ep, String film, String version, String url, String title) {
        LongDramaIngestRequest req = new LongDramaIngestRequest();
        req.setSourceSystem(SourceRefs.SYSTEM_LONG);
        req.setSourceWorkId(work);
        req.setSourceEpisodeId(ep);
        req.setSourceFilmId(film);
        req.setSourceVersion(version);
        req.setTitle(title);
        req.setVideoUrl(url);
        req.setIdempotencyKey("long:" + work + ":" + ep + ":" + film + ":v" + version);
        req.setDurationSec(100);
        req.setStatus(IngestStatus.READY);
        req.setApproved(Boolean.TRUE);
        req.setReviewNote("ok");
        return req;
    }

    private static ShortDramaIngestRequest shortSample(
            String work, String ep, String film, String url, String title) {
        ShortDramaIngestRequest req = new ShortDramaIngestRequest();
        req.setSourceSystem(SourceRefs.SYSTEM_SHORT);
        req.setSourceWorkId(work);
        req.setSourceEpisodeId(ep);
        req.setSourceFilmId(film);
        req.setTitle(title);
        req.setVideoUrl(url);
        req.setIdempotencyKey("short:" + work + ":" + ep + ":" + film);
        req.setDurationSec(10);
        req.setStatus(IngestStatus.READY);
        req.setApproved(Boolean.TRUE);
        return req;
    }

    private static String extractExternalRef(MediaDramaExample ex) {
        if (ex == null || ex.getOredCriteria() == null) return null;
        for (MediaDramaExample.Criteria c : ex.getOredCriteria()) {
            if (c == null || c.getCriteria() == null) continue;
            for (MediaDramaExample.Criterion crit : c.getCriteria()) {
                if (crit.getCondition() != null
                        && crit.getCondition().contains("external_ref")
                        && crit.getValue() != null) {
                    return String.valueOf(crit.getValue());
                }
            }
        }
        return null;
    }

    private static Long extractDramaId(MediaEpisodeExample ex) {
        if (ex == null || ex.getOredCriteria() == null) return null;
        for (MediaEpisodeExample.Criteria c : ex.getOredCriteria()) {
            if (c == null || c.getCriteria() == null) continue;
            for (MediaEpisodeExample.Criterion crit : c.getCriteria()) {
                if (crit.getCondition() != null
                        && crit.getCondition().contains("drama_id")
                        && crit.getValue() instanceof Long) {
                    return (Long) crit.getValue();
                }
                if (crit.getCondition() != null
                        && crit.getCondition().contains("drama_id")
                        && crit.getValue() instanceof Number) {
                    return ((Number) crit.getValue()).longValue();
                }
            }
        }
        return null;
    }

    private static Integer extractEpNo(MediaEpisodeExample ex) {
        if (ex == null || ex.getOredCriteria() == null) return null;
        for (MediaEpisodeExample.Criteria c : ex.getOredCriteria()) {
            if (c == null || c.getCriteria() == null) continue;
            for (MediaEpisodeExample.Criterion crit : c.getCriteria()) {
                if (crit.getCondition() != null
                        && crit.getCondition().contains("ep_no")
                        && crit.getValue() instanceof Integer) {
                    return (Integer) crit.getValue();
                }
                if (crit.getCondition() != null
                        && crit.getCondition().contains("ep_no")
                        && crit.getValue() instanceof Number) {
                    return ((Number) crit.getValue()).intValue();
                }
            }
        }
        return null;
    }

    private static MediaDrama copyDrama(MediaDrama d) {
        MediaDrama n = new MediaDrama();
        n.setId(d.getId()); n.setTitle(d.getTitle()); n.setCoverUrl(d.getCoverUrl());
        n.setExternalRef(d.getExternalRef()); n.setSource(d.getSource()); n.setStatus(d.getStatus());
        n.setDescription(d.getDescription());
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
    private static ObjectMapper snakeMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }
}
