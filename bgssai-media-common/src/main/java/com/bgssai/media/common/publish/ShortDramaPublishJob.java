package com.bgssai.media.common.publish;

import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.web.BizException;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical packager for the bgssai-short → bgssai-media publish loop.
 *
 * <p>short calls this (or a copy) when a finished episode has media asset refs.
 * Media ingest then upserts drama/episode. This is the export contract, not a studio.
 */
public final class ShortDramaPublishJob {

    public static final String SOURCE_SYSTEM = "bgssai-short";
    public static final String INGEST_PATH = "/bgssai/user/media/ingest/short-drama";
    public static final String TOKEN_HEADER = "X-Bgssai-Ingest-Token";

    private ShortDramaPublishJob() {
    }

    public static String idempotencyKey(String sourceWorkId, String sourceEpisodeId, String sourceFilmId) {
        return "short:" + trim(sourceWorkId) + ":" + trim(sourceEpisodeId) + ":" + trim(sourceFilmId);
    }

    /**
     * Package a finished short (metadata + media asset refs) for media ingest.
     */
    public static ShortDramaIngestRequest packageFinished(
            String sourceWorkId,
            String sourceEpisodeId,
            String sourceFilmId,
            String title,
            String coverUrl,
            String videoUrl,
            Integer durationSec,
            String aspectRatio,
            String language,
            List<String> tags) {
        require(sourceWorkId, "source_work_id");
        require(sourceEpisodeId, "source_episode_id");
        require(sourceFilmId, "source_film_id");
        require(title, "title");

        ShortDramaIngestRequest req = new ShortDramaIngestRequest();
        req.setSourceSystem(SOURCE_SYSTEM);
        req.setSourceWorkId(sourceWorkId.trim());
        req.setSourceEpisodeId(sourceEpisodeId.trim());
        req.setSourceFilmId(sourceFilmId.trim());
        req.setTitle(title.trim());
        req.setCoverUrl(blankToNull(coverUrl));
        req.setVideoUrl(videoUrl == null ? "" : videoUrl.trim());
        req.setDurationSec(durationSec == null ? 0 : durationSec);
        req.setAspectRatio(blankToDefault(aspectRatio, "9:16"));
        req.setLanguage(blankToDefault(language, "zh-CN"));
        req.setTags(tags == null ? new ArrayList<>() : new ArrayList<>(tags));
        req.setIdempotencyKey(idempotencyKey(sourceWorkId, sourceEpisodeId, sourceFilmId));
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

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String blankToDefault(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
