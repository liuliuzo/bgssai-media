package com.bgssai.media.common.publish;

import com.bgssai.media.common.dto.LongDramaIngestRequest;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.source.SourceRefs;
import com.bgssai.media.common.web.BizException;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical packager for the bgssai-long → bgssai-media publish loop (MEDIA-02).
 */
public final class LongDramaPublishJob {

    public static final String SOURCE_SYSTEM = SourceRefs.SYSTEM_LONG;
    public static final String INGEST_PATH = "/bgssai/user/media/ingest/long-drama";
    public static final String TOKEN_HEADER = "X-Bgssai-Ingest-Token";

    private LongDramaPublishJob() {
    }

    public static String idempotencyKey(
            String sourceWorkId, String sourceEpisodeId, String sourceFilmId, String sourceVersion) {
        return SourceRefs.longIdempotencyKey(sourceWorkId, sourceEpisodeId, sourceFilmId, sourceVersion);
    }

    public static LongDramaIngestRequest packageFinished(
            String sourceWorkId,
            String sourceEpisodeId,
            String sourceFilmId,
            String sourceVersion,
            String title,
            String coverUrl,
            String videoUrl,
            Integer durationSec,
            String aspectRatio,
            String language,
            List<String> tags,
            String reviewNote) {
        require(sourceWorkId, "source_work_id");
        require(sourceEpisodeId, "source_episode_id");
        require(sourceFilmId, "source_film_id");
        require(sourceVersion, "source_version");
        require(title, "title");

        LongDramaIngestRequest req = new LongDramaIngestRequest();
        req.setSourceSystem(SOURCE_SYSTEM);
        req.setSourceWorkId(sourceWorkId.trim());
        req.setSourceEpisodeId(sourceEpisodeId.trim());
        req.setSourceFilmId(sourceFilmId.trim());
        req.setSourceVersion(sourceVersion.trim());
        req.setTitle(title.trim());
        req.setCoverUrl(blankToNull(coverUrl));
        req.setVideoUrl(videoUrl == null ? "" : videoUrl.trim());
        req.setDurationSec(durationSec == null ? 0 : durationSec);
        req.setAspectRatio(blankToDefault(aspectRatio, "16:9"));
        req.setLanguage(blankToDefault(language, "zh-CN"));
        req.setTags(tags == null ? new ArrayList<>() : new ArrayList<>(tags));
        req.setIdempotencyKey(idempotencyKey(sourceWorkId, sourceEpisodeId, sourceFilmId, sourceVersion));
        req.setStatus(IngestStatus.READY);
        req.setApproved(Boolean.TRUE);
        req.setReviewNote(blankToNull(reviewNote));
        if (req.getDurationSec() < 0) {
            throw new BizException(400, "duration_sec must be >= 0");
        }
        return req;
    }

    private static void require(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new BizException(400, field + " required");
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String blankToDefault(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
