package com.bgssai.media.common.domain;

import java.util.Date;

public class MediaRecommendItem {
    private Long id;
    private Long runId;
    private Long videoId;
    private String stage;
    private String recallReason;
    private String filterReason;
    private Integer sortRank;
    private Integer kept;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getRecallReason() { return recallReason; }
    public void setRecallReason(String recallReason) { this.recallReason = recallReason; }

    public String getFilterReason() { return filterReason; }
    public void setFilterReason(String filterReason) { this.filterReason = filterReason; }

    public Integer getSortRank() { return sortRank; }
    public void setSortRank(Integer sortRank) { this.sortRank = sortRank; }

    public Integer getKept() { return kept; }
    public void setKept(Integer kept) { this.kept = kept; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
