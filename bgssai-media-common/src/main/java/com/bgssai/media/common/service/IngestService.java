package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.domain.MediaIngestLogExample;
import com.bgssai.media.common.ingest.IngestReadiness;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.ingest.MediaStorageGate;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IngestService {

    private final MediaDramaMapper mediaDramaMapper;
    private final MediaEpisodeMapper mediaEpisodeMapper;
    private final MediaIngestLogMapper mediaIngestLogMapper;
    private final ObjectMapper objectMapper;
    private final String ingestToken;
    private final MediaStorageGate mediaStorageGate;

    public IngestService(
            MediaDramaMapper mediaDramaMapper,
            MediaEpisodeMapper mediaEpisodeMapper,
            MediaIngestLogMapper mediaIngestLogMapper,
            ObjectMapper objectMapper,
            @Value("${bgssai.media.ingest.token}") String ingestToken,
            MediaStorageGate mediaStorageGate) {
        this.mediaDramaMapper = mediaDramaMapper;
        this.mediaEpisodeMapper = mediaEpisodeMapper;
        this.mediaIngestLogMapper = mediaIngestLogMapper;
        this.objectMapper = objectMapper;
        this.ingestToken = ingestToken;
        this.mediaStorageGate = mediaStorageGate;
    }

    public void assertToken(String token) {
        String expected = resolveExpectedToken();
        String profile = System.getProperty("spring.profiles.active", "");
        if (profile == null || profile.isBlank()) {
            profile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "");
        }
        boolean local = profile != null && profile.contains("local");
        if (expected == null || expected.isBlank()) {
            if (!local) {
                throw new BizException(503, "ingest token not configured");
            }
            throw new BizException(401, "invalid ingest token");
        }
        if (token == null || token.isBlank() || !expected.equals(token)) {
            throw new BizException(401, "invalid ingest token");
        }
    }

    public String resolveExpectedToken() {
        String env = System.getenv("MEDIA_INGEST_TOKEN");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return ingestToken;
    }

    @Transactional
    public Map<String, Object> publishFromShort(Map<String, Object> payload) {
        String externalRef = stringVal(payload.get("external_ref"));
        if (externalRef == null || externalRef.isBlank()) {
            throw new BizException(400, "external_ref required");
        }
        String title = stringVal(payload.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException(400, "title required");
        }

        IngestReadiness batchReadiness = evaluatePublishPayload(payload);
        if (batchReadiness.isFailed()) {
            MediaIngestLog log = new MediaIngestLog();
            log.setExternalRef(externalRef);
            log.setDramaId(null);
            try {
                log.setPayloadJson(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.setPayloadJson(String.valueOf(payload));
            }
            log.setStatus(IngestStatus.FAILED);
            log.setMessage(batchReadiness.getMessage());
            mediaIngestLogMapper.insertSelective(log);

            Map<String, Object> result = new HashMap<>();
            result.put("status", IngestStatus.FAILED);
            result.put("playable", false);
            result.put("message", batchReadiness.getMessage());
            result.put("ingest_log_id", log.getId());
            result.put("drama_id", null);
            result.put("external_ref", externalRef);
            return result;
        }

        String status = stringVal(payload.get("status"));
        if (status == null || status.isBlank()) status = "published";

        MediaDramaExample example = new MediaDramaExample();
        example.createCriteria().andExternalRefEqualTo(externalRef);
        List<MediaDrama> existing = mediaDramaMapper.selectByExample(example);
        MediaDrama drama;
        if (existing.isEmpty()) {
            drama = new MediaDrama();
            drama.setTitle(title);
            drama.setCoverUrl(stringVal(payload.get("cover_url")));
            drama.setDescription(stringVal(payload.get("description")));
            drama.setStatus(status);
            drama.setSource("short_publish");
            drama.setExternalRef(externalRef);
            mediaDramaMapper.insertSelective(drama);
        } else {
            drama = existing.get(0);
            MediaDrama patch = new MediaDrama();
            patch.setId(drama.getId());
            patch.setTitle(title);
            patch.setCoverUrl(stringVal(payload.get("cover_url")));
            patch.setDescription(stringVal(payload.get("description")));
            patch.setStatus(status);
            patch.setSource("short_publish");
            mediaDramaMapper.updateByPrimaryKeySelective(patch);
            drama = mediaDramaMapper.selectByPrimaryKey(drama.getId());
        }

        Object episodesObj = payload.get("episodes");
        if (episodesObj instanceof List<?> episodes) {
            for (Object item : episodes) {
                if (!(item instanceof Map<?, ?> epMapRaw)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> epMap = (Map<String, Object>) epMapRaw;
                Integer epNo = intVal(epMap.get("ep_no"));
                if (epNo == null) continue;
                MediaEpisodeExample epExample = new MediaEpisodeExample();
                epExample.createCriteria().andDramaIdEqualTo(drama.getId()).andEpNoEqualTo(epNo);
                List<MediaEpisode> epExisting = mediaEpisodeMapper.selectByExample(epExample);
                String epTitle = stringVal(epMap.get("title"));
                if (epTitle == null) epTitle = "EP" + epNo;
                String mediaUrl = stringVal(epMap.get("media_url"));
                String storageKey = stringVal(epMap.get("storage_key"));
                IngestReadiness epReady = IngestReadiness.evaluate(mediaStorageGate, mediaUrl, storageKey);
                String epStatus = epReady.isReady() ? "published" : "draft";
                if (epExisting.isEmpty()) {
                    MediaEpisode ep = new MediaEpisode();
                    ep.setDramaId(drama.getId());
                    ep.setEpNo(epNo);
                    ep.setTitle(epTitle);
                    ep.setDurationSec(intVal(epMap.get("duration_sec")));
                    ep.setMediaUrl(mediaUrl);
                    ep.setStorageKey(storageKey);
                    ep.setStatus(epStatus);
                    mediaEpisodeMapper.insertSelective(ep);
                } else {
                    MediaEpisode ep = epExisting.get(0);
                    MediaEpisode patch = new MediaEpisode();
                    patch.setId(ep.getId());
                    patch.setTitle(epTitle);
                    patch.setDurationSec(intVal(epMap.get("duration_sec")));
                    patch.setMediaUrl(mediaUrl);
                    patch.setStorageKey(storageKey);
                    patch.setStatus(epStatus);
                    mediaEpisodeMapper.updateByPrimaryKeySelective(patch);
                }
            }
        }

        MediaIngestLog log = new MediaIngestLog();
        log.setExternalRef(externalRef);
        log.setDramaId(drama.getId());
        try {
            log.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.setPayloadJson(String.valueOf(payload));
        }
        log.setStatus(IngestStatus.READY);
        log.setMessage(batchReadiness.getMessage());
        mediaIngestLogMapper.insertSelective(log);

        Map<String, Object> result = new HashMap<>();
        result.put("drama_id", drama.getId());
        result.put("ingest_log_id", log.getId());
        result.put("external_ref", externalRef);
        result.put("status", IngestStatus.READY);
        result.put("playable", true);
        return result;
    }

    /**
     * Batch publish fails closed: storage must be configured and at least one episode
     * must carry a resolvable asset reference. Empty episode list = missing asset.
     */
    IngestReadiness evaluatePublishPayload(Map<String, Object> payload) {
        String storageFail = mediaStorageGate.unconfiguredReason();
        if (storageFail != null) {
            return IngestReadiness.failed(storageFail);
        }
        Object episodesObj = payload.get("episodes");
        if (!(episodesObj instanceof List<?> episodes) || episodes.isEmpty()) {
            return IngestReadiness.failed("asset missing: episodes required with media_url or storage_key");
        }
        boolean anyReady = false;
        String lastFail = "asset missing: no episode with media_url or storage_key";
        for (Object item : episodes) {
            if (!(item instanceof Map<?, ?> epMapRaw)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> epMap = (Map<String, Object>) epMapRaw;
            IngestReadiness ep = IngestReadiness.evaluate(
                    mediaStorageGate, stringVal(epMap.get("media_url")), stringVal(epMap.get("storage_key")));
            if (ep.isReady()) {
                anyReady = true;
            } else {
                lastFail = ep.getMessage();
            }
        }
        if (!anyReady) {
            return IngestReadiness.failed(lastFail);
        }
        return IngestReadiness.ready("storage configured and at least one episode asset present");
    }

    public PageResult<MediaIngestLog> pageLogs(int pageNum, int pageSize) {
        MediaIngestLogExample example = new MediaIngestLogExample();
        example.setOrderByClause("id desc");
        PageHelper.startPage(pageNum, pageSize);
        List<MediaIngestLog> list = mediaIngestLogMapper.selectByExample(example);
        PageInfo<MediaIngestLog> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), pageNum, pageSize, list);
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer intVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
