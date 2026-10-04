package com.bgssai.media.user.supportbridge;

import java.util.Date;

/** 对应表 cs_support_outbox。统一客服出站队列，重启后仍可重试。 */
public class CsSupportOutbox {
    private Long id;
    private String eventKind;
    private String externalRef;
    private String requestPath;
    private String payloadJson;
    private String status;
    private Integer attemptCount;
    private String lastError;
    private Date nextAttemptAt;
    private Date sentAt;
    private Byte delFlag;
    private String creator;
    private String modifier;
    private Date gmtCreate;
    private Date gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventKind() { return eventKind; }
    public void setEventKind(String eventKind) { this.eventKind = eventKind; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Date getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Date nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
    public Byte getDelFlag() { return delFlag; }
    public void setDelFlag(Byte delFlag) { this.delFlag = delFlag; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public String getModifier() { return modifier; }
    public void setModifier(String modifier) { this.modifier = modifier; }
    public Date getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(Date gmtCreate) { this.gmtCreate = gmtCreate; }
    public Date getGmtModified() { return gmtModified; }
    public void setGmtModified(Date gmtModified) { this.gmtModified = gmtModified; }
}
