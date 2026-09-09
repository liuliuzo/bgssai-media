package com.bgssai.media.common.domain;

import java.util.Date;

public class MediaIngestLog {
    private Long id;
    private String externalRef;
    private Long dramaId;
    private String payloadJson;
    private String status;
    private String message;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public Long getDramaId() { return dramaId; }
    public void setDramaId(Long dramaId) { this.dramaId = dramaId; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
