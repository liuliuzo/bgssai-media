package com.bgssai.media.common.domain;

import java.util.Date;

public class MediaWatchProgress {
    private Long id;
    private Long userId;
    private Long dramaId;
    private Long episodeId;
    private Integer positionSec;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDramaId() { return dramaId; }
    public void setDramaId(Long dramaId) { this.dramaId = dramaId; }
    public Long getEpisodeId() { return episodeId; }
    public void setEpisodeId(Long episodeId) { this.episodeId = episodeId; }
    public Integer getPositionSec() { return positionSec; }
    public void setPositionSec(Integer positionSec) { this.positionSec = positionSec; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
