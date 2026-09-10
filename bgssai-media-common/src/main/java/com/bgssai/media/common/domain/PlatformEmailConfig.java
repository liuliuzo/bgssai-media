package com.bgssai.media.common.domain;

import java.util.Date;

/** 平台 SMTP 发信配置（platform_email_config 单例行 id=1）。 */
public class PlatformEmailConfig {
    private Long id;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String fromAddress;
    private String fromName;
    private Byte sslEnable;
    private Byte starttlsEnable;
    private Byte authEnable;
    private Byte enabled;
    private String lastTestStatus;
    private Date lastTestAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public Byte getSslEnable() { return sslEnable; }
    public void setSslEnable(Byte sslEnable) { this.sslEnable = sslEnable; }
    public Byte getStarttlsEnable() { return starttlsEnable; }
    public void setStarttlsEnable(Byte starttlsEnable) { this.starttlsEnable = starttlsEnable; }
    public Byte getAuthEnable() { return authEnable; }
    public void setAuthEnable(Byte authEnable) { this.authEnable = authEnable; }
    public Byte getEnabled() { return enabled; }
    public void setEnabled(Byte enabled) { this.enabled = enabled; }
    public String getLastTestStatus() { return lastTestStatus; }
    public void setLastTestStatus(String lastTestStatus) { this.lastTestStatus = lastTestStatus; }
    public Date getLastTestAt() { return lastTestAt; }
    public void setLastTestAt(Date lastTestAt) { this.lastTestAt = lastTestAt; }
}
