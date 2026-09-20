package com.bgssai.media.common.domain;

import java.util.Date;

public class MediaChannelFollow {
    private Long id;
    private Long followerUserId;
    private Long channelId;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFollowerUserId() { return followerUserId; }
    public void setFollowerUserId(Long followerUserId) { this.followerUserId = followerUserId; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
