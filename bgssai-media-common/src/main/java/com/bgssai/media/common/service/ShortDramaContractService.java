package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestReadiness;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.ingest.MediaStorageGate;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared bgssai-short publish contract mapped onto media_drama / media_episode.
 * MEDIA-01: only {@link IngestStatus#READY} is playable. Unconfigured storage or
 * missing asset → {@link IngestStatus#FAILED} (never fake READY / play_url).
 */
@Service
public class ShortDramaContractService {

    private final MediaDramaMapper mediaDramaMapper;
    private final MediaEpisodeMapper mediaEpisodeMapper;
    private final MediaIngestLogMapper mediaIngestLogMapper;
    private final ObjectMapper objectMapper;
    private final IngestService ingestService;
    private final MediaStorageGate mediaStorageGate;

    public ShortDramaContractService(
            MediaDramaMapper mediaDramaMapper,
            MediaEpisodeMapper mediaEpisodeMapper,
            MediaIngestLogMapper mediaIngestLogMapper,
            ObjectMapper objectMapper,
            IngestService ingestService,
            MediaStorageGate mediaStorageGate) {
        this.mediaDramaMapper = mediaDramaMapper;
        this.mediaEpisodeMapper = mediaEpisodeMapper;
        this.mediaIngestLogMapper = mediaIngestLogMapper;
        this.objectMapper = objectMapper;
        this.ingestService = ingestService;
        this.mediaStorageGate = mediaStorageGate;
    }

    public void assertToken(String token) {
        ingestService.assertToken(token);
    }

    @Transactional
    public Map<String, Object> ingest(ShortDramaIngestRequest req) {
        validate(req);
        String storageKey = "film:" + req.getSourceFilmId();
        IngestReadiness readiness = IngestReadiness.evaluate(
                mediaStorageGate, req.getVideoUrl(), storageKey);

        MediaIngestLog existing = mediaIngestLogMapper.selectByIdempotencyKey(req.getIdempotencyKey());
        try {
            if (readiness.isFailed()) {
                return recordFailed(existing, req, readiness.getMessage());
            }
            if (existing != null && existing.getMediaId() != null) {
                return updateExisting(existing, req, readiness);
            }
            if (existing != null) {
                return updateExisting(existing, req, readiness);
            }
            return insertNew(req, readiness);
        } catch (DuplicateKeyException race) {
            MediaIngestLog winner = mediaIngestLogMapper.selectByIdempotencyKey(req.getIdempotencyKey());
            if (winner == null) {
                throw new BizException(409, "idempotency race unresolved");
            }
            if (readiness.isFailed()) {
                return recordFailed(winner, req, readiness.getMessage());
            }
            return updateExisting(winner, req, readiness);
        }
    }

    private Map<String, Object> insertNew(ShortDramaIngestRequest req, IngestReadiness readiness) {
        String workRef = "short:work:" + req.getSourceWorkId();
        MediaDrama drama = upsertDrama(workRef, req, true);
        MediaEpisode episode = upsertEpisode(drama.getId(), req, true);
        String mediaId = "m_ep_" + episode.getId();
        String playUrl = req.getVideoUrl();

        MediaIngestLog log = new MediaIngestLog();
        log.setExternalRef(workRef);
        log.setDramaId(drama.getId());
        log.setIdempotencyKey(req.getIdempotencyKey());
        log.setMediaId(mediaId);
        log.setPlayUrl(playUrl);
        log.setStatus(IngestStatus.READY);
        log.setMessage(readiness.getMessage());
        log.setPayloadJson(toJson(req));
        mediaIngestLogMapper.insertSelective(log);
        return readyResult(mediaId, playUrl, log.getId());
    }

    private Map<String, Object> updateExisting(
            MediaIngestLog existing, ShortDramaIngestRequest req, IngestReadiness readiness) {
        String workRef = "short:work:" + req.getSourceWorkId();
        MediaDrama drama = upsertDrama(workRef, req, true);
        MediaEpisode episode = upsertEpisode(drama.getId(), req, true);
        String mediaId = existing.getMediaId() != null ? existing.getMediaId() : ("m_ep_" + episode.getId());
        String playUrl = req.getVideoUrl();
        existing.setExternalRef(workRef);
        existing.setDramaId(drama.getId());
        existing.setMediaId(mediaId);
        existing.setPlayUrl(playUrl);
        existing.setStatus(IngestStatus.READY);
        existing.setMessage(readiness.getMessage());
        existing.setPayloadJson(toJson(req));
        mediaIngestLogMapper.updateByIdempotencyKey(existing);
        return readyResult(mediaId, playUrl, existing.getId());
    }

    private Map<String, Object> recordFailed(
            MediaIngestLog existing, ShortDramaIngestRequest req, String reason) {
        String workRef = "short:work:" + req.getSourceWorkId();
        if (existing == null) {
            MediaIngestLog log = new MediaIngestLog();
            log.setExternalRef(workRef);
            log.setDramaId(null);
            log.setIdempotencyKey(req.getIdempotencyKey());
            log.setMediaId(null);
            log.setPlayUrl(null);
            log.setStatus(IngestStatus.FAILED);
            log.setMessage(reason);
            log.setPayloadJson(toJson(req));
            mediaIngestLogMapper.insertSelective(log);
            return failedResult(log.getId(), reason);
        }
        existing.setExternalRef(workRef);
        existing.setDramaId(null);
        existing.setMediaId(null);
        existing.setPlayUrl(null);
        existing.setStatus(IngestStatus.FAILED);
        existing.setMessage(reason);
        existing.setPayloadJson(toJson(req));
        mediaIngestLogMapper.updateByIdempotencyKey(existing);
        return failedResult(existing.getId(), reason);
    }

    private MediaDrama upsertDrama(String workRef, ShortDramaIngestRequest req, boolean playableReady) {
        MediaDramaExample example = new MediaDramaExample();
        example.createCriteria().andExternalRefEqualTo(workRef);
        List<MediaDrama> existing = mediaDramaMapper.selectByExample(example);
        String dramaStatus = playableReady ? "published" : "draft";
        if (existing.isEmpty()) {
            MediaDrama drama = new MediaDrama();
            drama.setTitle(req.getTitle());
            drama.setCoverUrl(req.getCoverUrl());
            drama.setDescription((req.getLanguage() == null ? "zh-CN" : req.getLanguage()) + " / "
                    + (req.getAspectRatio() == null ? "9:16" : req.getAspectRatio()));
            drama.setStatus(dramaStatus);
            drama.setSource("bgssai-short");
            drama.setExternalRef(workRef);
            mediaDramaMapper.insertSelective(drama);
            return mediaDramaMapper.selectByPrimaryKey(drama.getId());
        }
        MediaDrama drama = existing.get(0);
        MediaDrama patch = new MediaDrama();
        patch.setId(drama.getId());
        patch.setTitle(req.getTitle());
        patch.setCoverUrl(req.getCoverUrl());
        patch.setStatus(dramaStatus);
        patch.setSource("bgssai-short");
        mediaDramaMapper.updateByPrimaryKeySelective(patch);
        return mediaDramaMapper.selectByPrimaryKey(drama.getId());
    }

    private MediaEpisode upsertEpisode(Long dramaId, ShortDramaIngestRequest req, boolean playableReady) {
        int epNo = resolveEpNo(req.getSourceEpisodeId());
        MediaEpisodeExample epExample = new MediaEpisodeExample();
        epExample.createCriteria().andDramaIdEqualTo(dramaId).andEpNoEqualTo(epNo);
        List<MediaEpisode> epExisting = mediaEpisodeMapper.selectByExample(epExample);
        String storageKey = "film:" + req.getSourceFilmId();
        String epStatus = playableReady ? "published" : "draft";
        if (epExisting.isEmpty()) {
            MediaEpisode ep = new MediaEpisode();
            ep.setDramaId(dramaId);
            ep.setEpNo(epNo);
            ep.setTitle(req.getTitle());
            ep.setDurationSec(req.getDurationSec() == null ? 0 : req.getDurationSec());
            ep.setMediaUrl(req.getVideoUrl());
            ep.setStorageKey(storageKey);
            ep.setStatus(epStatus);
            mediaEpisodeMapper.insertSelective(ep);
            return mediaEpisodeMapper.selectByPrimaryKey(ep.getId());
        }
        MediaEpisode ep = epExisting.get(0);
        MediaEpisode patch = new MediaEpisode();
        patch.setId(ep.getId());
        patch.setTitle(req.getTitle());
        patch.setDurationSec(req.getDurationSec() == null ? 0 : req.getDurationSec());
        patch.setMediaUrl(req.getVideoUrl());
        patch.setStorageKey(storageKey);
        patch.setStatus(epStatus);
        mediaEpisodeMapper.updateByPrimaryKeySelective(patch);
        return mediaEpisodeMapper.selectByPrimaryKey(ep.getId());
    }

    public Map<String, Object> detail(String mediaId) {
        MediaIngestLog log = mediaIngestLogMapper.selectByMediaId(mediaId);
        if (log == null) {
            throw new BizException(404, "media not found");
        }
        String status = log.getStatus() == null ? IngestStatus.FAILED : log.getStatus();
        Map<String, Object> detail = new HashMap<>();
        detail.put("media_id", log.getMediaId());
        detail.put("status", status);
        detail.put("playable", IngestStatus.isPlayable(status));
        detail.put("storage_mode", mediaStorageGate.getMode().isEmpty() ? "UNCONFIGURED" : mediaStorageGate.getMode());
        detail.put("message", log.getMessage());
        // Never expose play_url unless READY — no fake playable success.
        if (IngestStatus.isPlayable(status) && log.getPlayUrl() != null && !log.getPlayUrl().isBlank()) {
            detail.put("play_url", log.getPlayUrl());
            detail.put("video_url", log.getPlayUrl());
        } else {
            detail.put("play_url", null);
            detail.put("video_url", null);
        }
        MediaDrama drama = log.getDramaId() == null ? null : mediaDramaMapper.selectByPrimaryKey(log.getDramaId());
        if (drama != null) {
            detail.put("title", drama.getTitle());
            detail.put("cover_url", drama.getCoverUrl());
            detail.put("source_system", drama.getSource());
        }
        mergePayload(detail, log.getPayloadJson());
        return detail;
    }

    public PageResult<Map<String, Object>> list(String q, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        long total = mediaIngestLogMapper.countContract();
        List<MediaIngestLog> logs = mediaIngestLogMapper.listRecentContract(pageSize, (page - 1) * pageSize);
        List<Map<String, Object>> list = new ArrayList<>();
        for (MediaIngestLog log : logs) {
            if (log.getMediaId() == null || log.getMediaId().isBlank()) {
                continue;
            }
            if (!IngestStatus.isPlayable(log.getStatus())) {
                continue;
            }
            Map<String, Object> item = detail(log.getMediaId());
            if (q != null && !q.isBlank()) {
                String hay = String.valueOf(item).toLowerCase();
                if (!hay.contains(q.toLowerCase())) {
                    continue;
                }
            }
            list.add(item);
        }
        return new PageResult<>(total, page, pageSize, list);
    }

    private void mergePayload(Map<String, Object> detail, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            putIfPresent(detail, "source_work_id", payload.get("source_work_id"));
            putIfPresent(detail, "source_episode_id", payload.get("source_episode_id"));
            putIfPresent(detail, "source_film_id", payload.get("source_film_id"));
            putIfPresent(detail, "aspect_ratio", payload.get("aspect_ratio"));
            putIfPresent(detail, "language", payload.get("language"));
            putIfPresent(detail, "duration_sec", payload.get("duration_sec"));
            putIfPresent(detail, "tags", payload.get("tags"));
            putIfPresent(detail, "title", payload.get("title"));
            putIfPresent(detail, "cover_url", payload.get("cover_url"));
            putIfPresent(detail, "source_system", payload.get("source_system"));
        } catch (Exception ignored) {
        }
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private static Map<String, Object> readyResult(String mediaId, String playUrl, Long ingestLogId) {
        Map<String, Object> result = new HashMap<>();
        result.put("media_id", mediaId);
        result.put("play_url", playUrl);
        result.put("status", IngestStatus.READY);
        result.put("playable", true);
        result.put("ingest_log_id", ingestLogId);
        return result;
    }

    private static Map<String, Object> failedResult(Long ingestLogId, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", IngestStatus.FAILED);
        result.put("playable", false);
        result.put("message", message);
        result.put("ingest_log_id", ingestLogId);
        result.put("play_url", null);
        result.put("media_id", null);
        return result;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new BizException(500, "json encode failed");
        }
    }

    private static int resolveEpNo(String sourceEpisodeId) {
        if (sourceEpisodeId == null || sourceEpisodeId.isBlank()) return 1;
        String digits = sourceEpisodeId.replaceAll("[^0-9]", "");
        if (!digits.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(digits));
            } catch (Exception ignored) {
            }
        }
        return Math.floorMod(sourceEpisodeId.hashCode(), 1_000_000) + 1;
    }

    private void validate(ShortDramaIngestRequest req) {
        require(req.getSourceSystem(), "source_system");
        require(req.getSourceWorkId(), "source_work_id");
        require(req.getSourceEpisodeId(), "source_episode_id");
        require(req.getSourceFilmId(), "source_film_id");
        require(req.getTitle(), "title");
        require(req.getIdempotencyKey(), "idempotency_key");
        // Allow blank video_url through to FAILED (missing asset) after storage check.
        if (req.getVideoUrl() != null && !req.getVideoUrl().isBlank()
                && !req.getVideoUrl().startsWith("http://")
                && !req.getVideoUrl().startsWith("https://")) {
            throw new BizException(400, "video_url must be http(s)");
        }
        if (req.getDurationSec() != null && req.getDurationSec() < 0) {
            throw new BizException(400, "duration_sec must be >= 0");
        }
        if (req.getDurationSec() == null) req.setDurationSec(0);
        if (req.getAspectRatio() == null || req.getAspectRatio().isBlank()) req.setAspectRatio("9:16");
        if (req.getLanguage() == null || req.getLanguage().isBlank()) req.setLanguage("zh-CN");
        if (req.getTags() == null) req.setTags(List.of());
    }

    private static void require(String v, String field) {
        if (v == null || v.isBlank()) throw new BizException(400, field + " required");
    }
}
