package com.bgssai.media.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public class MediaTranscodeRendition {
    private Long id;
    private Long jobId;
    private Long videoId;
    private String bitrateLabel;
    private Integer height;
    private String playUrl;
    private String storagePath;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getBitrateLabel() { return bitrateLabel; }
    public void setBitrateLabel(String bitrateLabel) { this.bitrateLabel = bitrateLabel; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getPlayUrl() { return playUrl; }
    public void setPlayUrl(String playUrl) { this.playUrl = playUrl; }

    @JsonIgnore
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
