package com.bgssai.media.common.domain;

import java.util.Date;

/** 登录验证码（media_verify_code）。code 只以 SHA-256 摘要落库。 */
public class MediaVerifyCode {
    private Long id;
    private String target;
    private String channel;
    private String scene;
    private String codeHash;
    private Integer attempts;
    private Boolean used;
    private Date expiresAt;
    private Date sentAt;
    private Date usedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
    public Date getUsedAt() { return usedAt; }
    public void setUsedAt(Date usedAt) { this.usedAt = usedAt; }
}
