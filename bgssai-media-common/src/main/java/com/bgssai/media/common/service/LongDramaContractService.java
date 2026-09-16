package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.dto.LongDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestReadiness;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.ingest.MediaStorageGate;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.bgssai.media.common.probe.MediaAssetProbe;
import com.bgssai.media.common.publish.LongDramaPublishJob;
import com.bgssai.media.common.source.SourceRefs;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MEDIA-02: bgssai-long publish contract on media_drama / media_episode.
 *
 * <p>Namespaces via {@link SourceRefs} so Long and Short identical upstream IDs do not collide.
 * Same episode + new {@code source_version} replaces assets on {@code (drama_id, ep_no)} while
 * keeping a stable {@code media_id}; same version retries are idempotent.
 */
@Service
public class LongDramaContractService {

    private final MediaDramaMapper mediaDramaMapper;
    private final MediaEpisodeMapper mediaEpisodeMapper;
    private final MediaIngestLogMapper mediaIngestLogMapper;
    private final ObjectMapper objectMapper;
    private final IngestService ingestService;
    private final MediaStorageGate mediaStorageGate;
    private final MediaAssetProbe mediaAssetProbe;

    public LongDramaContractService(
            MediaDramaMapper mediaDramaMapper,
            MediaEpisodeMapper mediaEpisodeMapper,
            MediaIngestLogMapper mediaIngestLogMapper,
            ObjectMapper objectMapper,
            IngestService ingestService,
            MediaStorageGate mediaStorageGate,
            MediaAssetProbe mediaAssetProbe) {
        this.mediaDramaMapper = mediaDramaMapper;
        this.mediaEpisodeMapper = mediaEpisodeMapper;
        this.mediaIngestLogMapper = mediaIngestLogMapper;
        this.objectMapper = objectMapper;
        this.ingestService = ingestService;
        this.mediaStorageGate = mediaStorageGate;
        this.mediaAssetProbe = mediaAssetProbe;
    }

    public void assertToken(String token) {
        ingestService.assertToken(token);
    }

    @Transactional
    public Map<String, Object> ingest(LongDramaIngestRequest req) {
        validate(req);
        assertApprovedReadyPack(req);
        String storageKey = SourceRefs.filmStorageKey(SourceRefs.NS_LONG, req.getSourceFilmId());
        IngestReadiness readiness = IngestReadiness.evaluate(
                mediaStorageGate, mediaAssetProbe, req.getVideoUrl(), storageKey);

        MediaIngestLog existing = mediaIngestLogMapper.selectByIdempotencyKey(req.getIdempotencyKey());
        if (isReadyCatalog(existing)) {
            return replayReady(existing);
        }
        try {
            if (readiness.isFailed()) {
                return recordFailed(existing, req, readiness.getMessage());
            }
            if (readiness.isPending()) {
                return recordPending(existing, req, readiness.getMessage());
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
            if (isReadyCatalog(winner)) {
                return replayReady(winner);
            }
            if (readiness.isFailed()) {
                return recordFailed(winner, req, readiness.getMessage());
            }
            if (readiness.isPending()) {
                return recordPending(winner, req, readiness.getMessage());
            }
            return updateExisting(winner, req, readiness);
        }
    }

    private Map<String, Object> insertNew(LongDramaIngestRequest req, IngestReadiness readiness) {
        String workRef = SourceRefs.workRef(SourceRefs.NS_LONG, req.getSourceWorkId());
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
        return readyResult(mediaId, playUrl, log.getId(), req.getIdempotencyKey(), false, req);
    }

    private Map<String, Object> updateExisting(
            MediaIngestLog existing, LongDramaIngestRequest req, IngestReadiness readiness) {
        String workRef = SourceRefs.workRef(SourceRefs.NS_LONG, req.getSourceWorkId());
        MediaDrama drama = upsertDrama(workRef, req, true);
        MediaEpisode episode = upsertEpisode(drama.getId(), req, true);
        String mediaId = existing.getMediaId() != null ? existing.getMediaId() : ("m_ep_" + episode.getId());
        // Prefer stable episode media_id when replacing versions of the same ep.
        if (episode.getId() != null) {
            mediaId = "m_ep_" + episode.getId();
        }
        String playUrl = req.getVideoUrl();
        existing.setExternalRef(workRef);
        existing.setDramaId(drama.getId());
        existing.setMediaId(mediaId);
        existing.setPlayUrl(playUrl);
        existing.setStatus(IngestStatus.READY);
        existing.setMessage(readiness.getMessage());
        existing.setPayloadJson(toJson(req));
        mediaIngestLogMapper.updateByIdempotencyKey(existing);
        return readyResult(mediaId, playUrl, existing.getId(), req.getIdempotencyKey(), false, req);
    }

    /**
     * MEDIA-01: asset unverified so far (origin timed out, 5xx, or storage_key only). Held under
     * the same idempotency key so a retry updates this row, and no media_id / play_url leaks out.
     */
    private Map<String, Object> recordPending(
            MediaIngestLog existing, LongDramaIngestRequest req, String reason) {
        String workRef = SourceRefs.workRef(SourceRefs.NS_LONG, req.getSourceWorkId());
        if (existing == null) {
            MediaIngestLog log = new MediaIngestLog();
            log.setExternalRef(workRef);
            log.setDramaId(null);
            log.setIdempotencyKey(req.getIdempotencyKey());
            log.setMediaId(null);
            log.setPlayUrl(null);
            log.setStatus(IngestStatus.PENDING);
            log.setMessage(reason);
            log.setPayloadJson(toJson(req));
            mediaIngestLogMapper.insertSelective(log);
            return pendingResult(log.getId(), reason, req.getIdempotencyKey());
        }
        existing.setExternalRef(workRef);
        existing.setDramaId(null);
        existing.setMediaId(null);
        existing.setPlayUrl(null);
        existing.setStatus(IngestStatus.PENDING);
        existing.setMessage(reason);
        existing.setPayloadJson(toJson(req));
        mediaIngestLogMapper.updateByIdempotencyKey(existing);
        return pendingResult(existing.getId(), reason, req.getIdempotencyKey());
    }

    private Map<String, Object> recordFailed(
            MediaIngestLog existing, LongDramaIngestRequest req, String reason) {
        String workRef = SourceRefs.workRef(SourceRefs.NS_LONG, req.getSourceWorkId());
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
            return failedResult(log.getId(), reason, req.getIdempotencyKey());
        }
        existing.setExternalRef(workRef);
        existing.setDramaId(null);
        existing.setMediaId(null);
        existing.setPlayUrl(null);
        existing.setStatus(IngestStatus.FAILED);
        existing.setMessage(reason);
        existing.setPayloadJson(toJson(req));
        mediaIngestLogMapper.updateByIdempotencyKey(existing);
        return failedResult(existing.getId(), reason, req.getIdempotencyKey());
    }

    private MediaDrama upsertDrama(String workRef, LongDramaIngestRequest req, boolean playableReady) {
        MediaDramaExample example = new MediaDramaExample();
        example.createCriteria().andExternalRefEqualTo(workRef);
        List<MediaDrama> existing = mediaDramaMapper.selectByExample(example);
        String dramaStatus = playableReady ? "published" : "draft";
        if (existing.isEmpty()) {
            MediaDrama drama = new MediaDrama();
            drama.setTitle(req.getTitle());
            drama.setCoverUrl(req.getCoverUrl());
            drama.setDescription((req.getLanguage() == null ? "zh-CN" : req.getLanguage()) + " / "
                    + (req.getAspectRatio() == null ? "16:9" : req.getAspectRatio()));
            drama.setStatus(dramaStatus);
            drama.setSource(SourceRefs.SYSTEM_LONG);
            drama.setExternalRef(workRef);
            try {
                mediaDramaMapper.insertSelective(drama);
                return mediaDramaMapper.selectByPrimaryKey(drama.getId());
            } catch (DuplicateKeyException race) {
                MediaDramaExample raced = new MediaDramaExample();
                raced.createCriteria().andExternalRefEqualTo(workRef);
                List<MediaDrama> winner = mediaDramaMapper.selectByExample(raced);
                if (winner.isEmpty()) {
                    throw race;
                }
                existing = winner;
            }
        }
        MediaDrama drama = existing.get(0);
        MediaDrama patch = new MediaDrama();
        patch.setId(drama.getId());
        patch.setTitle(req.getTitle());
        patch.setCoverUrl(req.getCoverUrl());
        patch.setStatus(dramaStatus);
        patch.setSource(SourceRefs.SYSTEM_LONG);
        mediaDramaMapper.updateByPrimaryKeySelective(patch);
        return mediaDramaMapper.selectByPrimaryKey(drama.getId());
    }

    private MediaEpisode upsertEpisode(Long dramaId, LongDramaIngestRequest req, boolean playableReady) {
        int epNo = resolveEpNo(req.getSourceEpisodeId());
        MediaEpisodeExample epExample = new MediaEpisodeExample();
        epExample.createCriteria().andDramaIdEqualTo(dramaId).andEpNoEqualTo(epNo);
        List<MediaEpisode> epExisting = mediaEpisodeMapper.selectByExample(epExample);
        String storageKey = SourceRefs.filmStorageKey(SourceRefs.NS_LONG, req.getSourceFilmId());
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
            try {
                mediaEpisodeMapper.insertSelective(ep);
                return mediaEpisodeMapper.selectByPrimaryKey(ep.getId());
            } catch (DuplicateKeyException race) {
                MediaEpisodeExample raced = new MediaEpisodeExample();
                raced.createCriteria().andDramaIdEqualTo(dramaId).andEpNoEqualTo(epNo);
                List<MediaEpisode> winner = mediaEpisodeMapper.selectByExample(raced);
                if (winner.isEmpty()) {
                    throw race;
                }
                epExisting = winner;
            }
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
        if (log == null || !SourceRefs.isLongWorkRef(log.getExternalRef())) {
            throw new BizException(404, "media not found");
        }
        String status = log.getStatus() == null ? IngestStatus.FAILED : log.getStatus();
        Map<String, Object> detail = new HashMap<>();
        detail.put("media_id", log.getMediaId());
        detail.put("status", status);
        detail.put("playable", IngestStatus.isPlayable(status));
        detail.put("idempotency_key", log.getIdempotencyKey());
        detail.put("storage_mode", mediaStorageGate.getMode().isEmpty() ? "UNCONFIGURED" : mediaStorageGate.getMode());
        detail.put("message", log.getMessage());
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
            detail.put("drama_id", drama.getId());
            detail.put("external_ref", drama.getExternalRef());
        }
        mergePayload(detail, log.getPayloadJson());
        return detail;
    }

    public PageResult<Map<String, Object>> list(String q, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        String prefix = SourceRefs.contractPrefix(SourceRefs.NS_LONG);
        long total = mediaIngestLogMapper.countContractByPrefix(prefix);
        List<MediaIngestLog> logs = mediaIngestLogMapper.listRecentContractByPrefix(
                prefix, pageSize, (page - 1) * pageSize);
        List<Map<String, Object>> list = new ArrayList<>();
        Set<String> seenMediaIds = new HashSet<>();
        for (MediaIngestLog log : logs) {
            if (log.getMediaId() == null || log.getMediaId().isBlank()) {
                continue;
            }
            if (!IngestStatus.isPlayable(log.getStatus())) {
                continue;
            }
            if (!seenMediaIds.add(log.getMediaId())) {
                continue;
            }
            Map<String, Object> item = detail(log.getMediaId());
            item.put("idempotency_key", log.getIdempotencyKey());
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

    /**
     * Cross-episode query for one Long work, ordered by {@code ep_no} ascending.
     * Each item is the latest READY cut for that episode (stable media_id).
     */
    public Map<String, Object> listEpisodesByWork(String sourceWorkId) {
        require(sourceWorkId, "source_work_id");
        String workRef = SourceRefs.workRef(SourceRefs.NS_LONG, sourceWorkId);
        MediaDramaExample example = new MediaDramaExample();
        example.createCriteria().andExternalRefEqualTo(workRef);
        List<MediaDrama> dramas = mediaDramaMapper.selectByExample(example);
        if (dramas.isEmpty()) {
            throw new BizException(404, "work not found");
        }
        MediaDrama drama = dramas.get(0);
        MediaEpisodeExample epExample = new MediaEpisodeExample();
        epExample.createCriteria().andDramaIdEqualTo(drama.getId());
        epExample.setOrderByClause("ep_no asc");
        List<MediaEpisode> episodes = mediaEpisodeMapper.selectByExample(epExample);
        List<Map<String, Object>> items = new ArrayList<>();
        for (MediaEpisode ep : episodes) {
            String mediaId = "m_ep_" + ep.getId();
            Map<String, Object> item = new HashMap<>();
            item.put("episode_id", ep.getId());
            item.put("ep_no", ep.getEpNo());
            item.put("title", ep.getTitle());
            item.put("duration_sec", ep.getDurationSec());
            item.put("status", ep.getStatus());
            item.put("media_id", mediaId);
            item.put("storage_key", ep.getStorageKey());
            MediaIngestLog log = mediaIngestLogMapper.selectByMediaId(mediaId);
            if (log != null && IngestStatus.isPlayable(log.getStatus())) {
                item.put("playable", true);
                item.put("play_url", log.getPlayUrl());
                item.put("ingest_status", log.getStatus());
                item.put("idempotency_key", log.getIdempotencyKey());
                mergePayload(item, log.getPayloadJson());
            } else {
                item.put("playable", false);
                item.put("play_url", null);
                item.put("ingest_status", log == null ? null : log.getStatus());
            }
            items.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("source_work_id", sourceWorkId.trim());
        result.put("external_ref", workRef);
        result.put("drama_id", drama.getId());
        result.put("title", drama.getTitle());
        result.put("cover_url", drama.getCoverUrl());
        result.put("source_system", drama.getSource());
        result.put("episodes", items);
        return result;
    }

    private void mergePayload(Map<String, Object> detail, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            putIfPresent(detail, "source_work_id", payload.get("source_work_id"));
            putIfPresent(detail, "source_episode_id", payload.get("source_episode_id"));
            putIfPresent(detail, "source_film_id", payload.get("source_film_id"));
            putIfPresent(detail, "source_version", payload.get("source_version"));
            putIfPresent(detail, "aspect_ratio", payload.get("aspect_ratio"));
            putIfPresent(detail, "language", payload.get("language"));
            putIfPresent(detail, "duration_sec", payload.get("duration_sec"));
            putIfPresent(detail, "tags", payload.get("tags"));
            putIfPresent(detail, "title", payload.get("title"));
            putIfPresent(detail, "cover_url", payload.get("cover_url"));
            putIfPresent(detail, "source_system", payload.get("source_system"));
            putIfPresent(detail, "review_note", payload.get("review_note"));
            putIfPresent(detail, "approved", payload.get("approved"));
        } catch (Exception ignored) {
        }
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private static boolean isReadyCatalog(MediaIngestLog log) {
        return log != null
                && log.getMediaId() != null
                && !log.getMediaId().isBlank()
                && IngestStatus.isPlayable(log.getStatus());
    }

    private Map<String, Object> replayReady(MediaIngestLog existing) {
        Map<String, Object> result = readyResult(
                existing.getMediaId(),
                existing.getPlayUrl(),
                existing.getId(),
                existing.getIdempotencyKey(),
                true,
                null);
        mergePayload(result, existing.getPayloadJson());
        return result;
    }

    private static Map<String, Object> readyResult(
            String mediaId,
            String playUrl,
            Long ingestLogId,
            String idempotencyKey,
            boolean replayed,
            LongDramaIngestRequest req) {
        Map<String, Object> result = new HashMap<>();
        result.put("media_id", mediaId);
        result.put("play_url", playUrl);
        result.put("status", IngestStatus.READY);
        result.put("playable", true);
        result.put("ingest_log_id", ingestLogId);
        result.put("idempotency_key", idempotencyKey);
        result.put("replayed", replayed);
        if (req != null) {
            result.put("source_work_id", req.getSourceWorkId());
            result.put("source_episode_id", req.getSourceEpisodeId());
            result.put("source_film_id", req.getSourceFilmId());
            result.put("source_version", req.getSourceVersion());
            result.put("source_system", req.getSourceSystem());
        }
        return result;
    }

    private static Map<String, Object> pendingResult(
            Long ingestLogId, String message, String idempotencyKey) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", IngestStatus.PENDING);
        result.put("playable", false);
        result.put("retryable", true);
        result.put("message", message);
        result.put("ingest_log_id", ingestLogId);
        result.put("play_url", null);
        result.put("media_id", null);
        result.put("replayed", false);
        result.put("idempotency_key", idempotencyKey);
        return result;
    }

    private static Map<String, Object> failedResult(Long ingestLogId, String message, String idempotencyKey) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", IngestStatus.FAILED);
        result.put("playable", false);
        result.put("message", message);
        result.put("ingest_log_id", ingestLogId);
        result.put("play_url", null);
        result.put("media_id", null);
        result.put("replayed", false);
        result.put("idempotency_key", idempotencyKey);
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

    private void validate(LongDramaIngestRequest req) {
        require(req.getSourceSystem(), "source_system");
        if (!SourceRefs.SYSTEM_LONG.equals(req.getSourceSystem().trim())) {
            throw new BizException(400, "source_system must be bgssai-long");
        }
        req.setSourceSystem(SourceRefs.SYSTEM_LONG);
        require(req.getSourceWorkId(), "source_work_id");
        require(req.getSourceEpisodeId(), "source_episode_id");
        require(req.getSourceFilmId(), "source_film_id");
        require(req.getSourceVersion(), "source_version");
        require(req.getTitle(), "title");
        require(req.getIdempotencyKey(), "idempotency_key");
        String expectedKey = LongDramaPublishJob.idempotencyKey(
                req.getSourceWorkId(), req.getSourceEpisodeId(), req.getSourceFilmId(), req.getSourceVersion());
        if (!expectedKey.equals(req.getIdempotencyKey().trim())) {
            throw new BizException(400,
                    "idempotency_key must be long:{source_work_id}:{source_episode_id}:{source_film_id}:v{source_version}");
        }
        req.setIdempotencyKey(expectedKey);
        if (req.getVideoUrl() != null && !req.getVideoUrl().isBlank()
                && !req.getVideoUrl().startsWith("http://")
                && !req.getVideoUrl().startsWith("https://")) {
            throw new BizException(400, "video_url must be http(s)");
        }
        if (req.getDurationSec() != null && req.getDurationSec() < 0) {
            throw new BizException(400, "duration_sec must be >= 0");
        }
        if (req.getDurationSec() == null) req.setDurationSec(0);
        if (req.getAspectRatio() == null || req.getAspectRatio().isBlank()) req.setAspectRatio("16:9");
        if (req.getLanguage() == null || req.getLanguage().isBlank()) req.setLanguage("zh-CN");
        if (req.getTags() == null) req.setTags(List.of());
    }

    private static void require(String v, String field) {
        if (v == null || v.isBlank()) throw new BizException(400, field + " required");
    }

    void assertApprovedReadyPack(LongDramaIngestRequest req) {
        if (req.getApproved() != null && !req.getApproved()) {
            throw new BizException(422, "pack not approved");
        }
        String status = req.getStatus();
        if (status == null || status.isBlank()) {
            return;
        }
        String normalized = status.trim();
        if (IngestStatus.READY.equalsIgnoreCase(normalized)
                || "APPROVED".equalsIgnoreCase(normalized)) {
            return;
        }
        throw new BizException(422, "only READY approved packs are accepted");
    }
}
