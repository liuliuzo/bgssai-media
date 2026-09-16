package com.bgssai.media.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * MEDIA-02 long-drama publish body (snake_case JSON).
 * Mirrors Short fields plus {@code source_version} / optional review note.
 */
public class LongDramaIngestRequest {
    private String sourceSystem;
    private String sourceWorkId;
    private String sourceEpisodeId;
    private String sourceFilmId;
    /** Cut / export version within the same episode (required). */
    private String sourceVersion;
    private String title;
    private String coverUrl;
    private String videoUrl;
    private Integer durationSec = 0;
    private String aspectRatio = "16:9";
    private String language = "zh-CN";
    private List<String> tags = new ArrayList<>();
    private String idempotencyKey;
    /** Optional pack status (READY / APPROVED). Omitted = approved READY pack. */
    private String status;
    /** Optional approval flag. false rejected; null/true accepted. */
    private Boolean approved;
    /** Optional human / pipeline review note recorded in payload. */
    private String reviewNote;

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getSourceWorkId() { return sourceWorkId; }
    public void setSourceWorkId(String sourceWorkId) { this.sourceWorkId = sourceWorkId; }
    public String getSourceEpisodeId() { return sourceEpisodeId; }
    public void setSourceEpisodeId(String sourceEpisodeId) { this.sourceEpisodeId = sourceEpisodeId; }
    public String getSourceFilmId() { return sourceFilmId; }
    public void setSourceFilmId(String sourceFilmId) { this.sourceFilmId = sourceFilmId; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
