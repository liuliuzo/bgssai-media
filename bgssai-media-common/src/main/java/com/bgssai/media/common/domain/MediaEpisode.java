package com.bgssai.media.common.domain;

import java.util.Date;

public class MediaEpisode {
    private Long id;
    private Long dramaId;
    private Integer epNo;
    private String title;
    private Integer durationSec;
    private String mediaUrl;
    private String storageKey;
    private String status;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDramaId() { return dramaId; }
    public void setDramaId(Long dramaId) { this.dramaId = dramaId; }
    public Integer getEpNo() { return epNo; }
    public void setEpNo(Integer epNo) { this.epNo = epNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
