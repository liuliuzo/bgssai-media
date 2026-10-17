package com.bgssai.media.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class MediaVideoComment {
    private Long id;
    private Long videoId;
    private Long userId;
    private Long parentId;
    private String content;
    /** DB tinyint 0/1; JSON exposes boolean via {@link #isPinned()}. */
    private Integer pinned;
    private Date deletedAt;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @JsonIgnore
    public Integer getPinned() { return pinned; }

    public void setPinned(Integer pinned) { this.pinned = pinned; }

    @JsonProperty("pinned")
    public boolean isPinned() {
        return pinned != null && pinned != 0;
    }

    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
