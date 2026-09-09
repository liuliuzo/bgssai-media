package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.dto.ShortDramaIngestRequest;
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
 * Shared bgssai-short publish contract, mapped onto media_drama / media_episode.
 * MVP storage: REFERENCE (play_url = video_url).
 */
@Service
public class ShortDramaContractService {

    private final MediaDramaMapper mediaDramaMapper;
    private final MediaEpisodeMapper mediaEpisodeMapper;
    private final MediaIngestLogMapper mediaIngestLogMapper;
    private final ObjectMapper objectMapper;
    private final IngestService ingestService;

    public ShortDramaContractService(
            MediaDramaMapper mediaDramaMapper,
            MediaEpisodeMapper mediaEpisodeMapper,
            MediaIngestLogMapper mediaIngestLogMapper,
            ObjectMapper objectMapper,
            IngestService ingestService) {
        this.mediaDramaMapper = mediaDramaMapper;
        this.mediaEpisodeMapper = mediaEpisodeMapper;
        this.mediaIngestLogMapper = mediaIngestLogMapper;
        this.objectMapper = objectMapper;
        this.ingestService = ingestService;
    }

    public void assertToken(String token) {
        ingestService.assertToken(token);
    }

    @Transactional
    public Map<String, Object> ingest(ShortDramaIngestRequest req) {
        validate(req);
        MediaIngestLog existing = mediaIngestLogMapper.selectByIdempotencyKey(req.getIdempotencyKey());
        try {
            if (existing != null && existing.getMediaId() != null) {
                return updateExisting(existing, req);
            }
            return insertNew(req);
        } catch (DuplicateKeyException race) {
            MediaIngestLog winner = mediaIngestLogMapper.selectByIdempotencyKey(req.getIdempotencyKey());
            if (winner == null) {
                throw new BizException(409, "idempotency race unresolved");
            }
            return updateExisting(winner, req);
        }
    }

    private Map<String, Object> insertNew(ShortDramaIngestRequest req) {
        String workRef = "short:work:" + req.getSourceWorkId();
        MediaDrama drama = upsertDrama(workRef, req);
        MediaEpisode episode = upsertEpisode(drama.getId(), req);
        String mediaId = "m_ep_" + episode.getId();
        String playUrl = req.getVideoUrl();

        MediaIngestLog log = new MediaIngestLog();
        log.setExternalRef(workRef);
        log.setDramaId(drama.getId());
        log.setIdempotencyKey(req.getIdempotencyKey());
        log.setMediaId(mediaId);
        log.setPlayUrl(playUrl);
        log.setStatus("PUBLISHED");
        log.setMessage("short-drama contract accepted");
        log.setPayloadJson(toJson(req));
        mediaIngestLogMapper.insertSelective(log);
        return result(mediaId, playUrl);
    }

    private Map<String, Object> updateExisting(MediaIngestLog existing, ShortDramaIngestRequest req) {
        String workRef = "short:work:" + req.getSourceWorkId();
        MediaDrama drama = upsertDrama(workRef, req);
        MediaEpisode episode = upsertEpisode(drama.getId(), req);
        String mediaId = existing.getMediaId() != null ? existing.getMediaId() : ("m_ep_" + episode.getId());
        String playUrl = req.getVideoUrl();
        existing.setExternalRef(workRef);
        existing.setDramaId(drama.getId());
        existing.setMediaId(mediaId);
        existing.setPlayUrl(playUrl);
        existing.setStatus("PUBLISHED");
        existing.setMessage("short-drama contract re-ingest");
        existing.setPayloadJson(toJson(req));
        mediaIngestLogMapper.updateByIdempotencyKey(existing);
        return result(mediaId, playUrl);
    }

    private MediaDrama upsertDrama(String workRef, ShortDramaIngestRequest req) {
        MediaDramaExample example = new MediaDramaExample();
        example.createCriteria().andExternalRefEqualTo(workRef);
        List<MediaDrama> existing = mediaDramaMapper.selectByExample(example);
        if (existing.isEmpty()) {
            MediaDrama drama = new MediaDrama();
            drama.setTitle(req.getTitle());
            drama.setCoverUrl(req.getCoverUrl());
            drama.setDescription((req.getLanguage() == null ? "zh-CN" : req.getLanguage()) + " / "
                    + (req.getAspectRatio() == null ? "9:16" : req.getAspectRatio()));
            drama.setStatus("published");
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
        patch.setStatus("published");
        patch.setSource("bgssai-short");
        mediaDramaMapper.updateByPrimaryKeySelective(patch);
        return mediaDramaMapper.selectByPrimaryKey(drama.getId());
    }

    private MediaEpisode upsertEpisode(Long dramaId, ShortDramaIngestRequest req) {
        int epNo = resolveEpNo(req.getSourceEpisodeId());
        MediaEpisodeExample epExample = new MediaEpisodeExample();
        epExample.createCriteria().andDramaIdEqualTo(dramaId).andEpNoEqualTo(epNo);
        List<MediaEpisode> epExisting = mediaEpisodeMapper.selectByExample(epExample);
        String storageKey = "film:" + req.getSourceFilmId();
        if (epExisting.isEmpty()) {
            MediaEpisode ep = new MediaEpisode();
            ep.setDramaId(dramaId);
            ep.setEpNo(epNo);
            ep.setTitle(req.getTitle());
            ep.setDurationSec(req.getDurationSec() == null ? 0 : req.getDurationSec());
            ep.setMediaUrl(req.getVideoUrl());
            ep.setStorageKey(storageKey);
            ep.setStatus("published");
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
        patch.setStatus("published");
        mediaEpisodeMapper.updateByPrimaryKeySelective(patch);
        return mediaEpisodeMapper.selectByPrimaryKey(ep.getId());
    }

    public Map<String, Object> detail(String mediaId) {
        MediaIngestLog log = mediaIngestLogMapper.selectByMediaId(mediaId);
        if (log == null || log.getPlayUrl() == null) {
            throw new BizException(404, "media not found");
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("media_id", log.getMediaId());
        detail.put("play_url", log.getPlayUrl());
        detail.put("video_url", log.getPlayUrl());
        detail.put("status", log.getStatus() == null ? "PUBLISHED" : log.getStatus());
        detail.put("storage_mode", "REFERENCE");
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

    private static Map<String, Object> result(String mediaId, String playUrl) {
        Map<String, Object> result = new HashMap<>();
        result.put("media_id", mediaId);
        result.put("play_url", playUrl);
        result.put("status", "PUBLISHED");
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
        require(req.getVideoUrl(), "video_url");
        require(req.getIdempotencyKey(), "idempotency_key");
        if (!req.getVideoUrl().startsWith("http://") && !req.getVideoUrl().startsWith("https://")) {
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
