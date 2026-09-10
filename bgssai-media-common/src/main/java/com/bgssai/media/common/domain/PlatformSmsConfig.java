package com.bgssai.media.common.domain;

import java.util.Date;

/** 平台短信通道配置（platform_sms_config 单例行 id=1）。 */
public class PlatformSmsConfig {
    private Long id;
    private String provider;
    private String secretId;
    private String secretKey;
    private String sdkAppId;
    private String signName;
    private String region;
    private String loginTemplateId;
    private String bindPhoneTemplateId;
    private String resetPasswordTemplateId;
    private Integer codeExpireSeconds;
    private Byte enabled;
    private String lastTestStatus;
    private Date lastTestAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getSecretId() { return secretId; }
    public void setSecretId(String secretId) { this.secretId = secretId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getSdkAppId() { return sdkAppId; }
    public void setSdkAppId(String sdkAppId) { this.sdkAppId = sdkAppId; }
    public String getSignName() { return signName; }
    public void setSignName(String signName) { this.signName = signName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getLoginTemplateId() { return loginTemplateId; }
    public void setLoginTemplateId(String loginTemplateId) { this.loginTemplateId = loginTemplateId; }
    public String getBindPhoneTemplateId() { return bindPhoneTemplateId; }
    public void setBindPhoneTemplateId(String bindPhoneTemplateId) { this.bindPhoneTemplateId = bindPhoneTemplateId; }
    public String getResetPasswordTemplateId() { return resetPasswordTemplateId; }
    public void setResetPasswordTemplateId(String resetPasswordTemplateId) { this.resetPasswordTemplateId = resetPasswordTemplateId; }
    public Integer getCodeExpireSeconds() { return codeExpireSeconds; }
    public void setCodeExpireSeconds(Integer codeExpireSeconds) { this.codeExpireSeconds = codeExpireSeconds; }
    public Byte getEnabled() { return enabled; }
    public void setEnabled(Byte enabled) { this.enabled = enabled; }
    public String getLastTestStatus() { return lastTestStatus; }
    public void setLastTestStatus(String lastTestStatus) { this.lastTestStatus = lastTestStatus; }
    public Date getLastTestAt() { return lastTestAt; }
    public void setLastTestAt(Date lastTestAt) { this.lastTestAt = lastTestAt; }
}
