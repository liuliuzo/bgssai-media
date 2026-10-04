package com.bgssai.media.user.supportbridge;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/** 用户端到统一客服平台的出站配置。默认关闭。密钥只来自配置。 */
@ConfigurationProperties(prefix = "bgssai.media.support")
public class SupportBridgeProperties {

    private boolean enabled = false;
    private String baseUrl = "";
    private String productCode = "media";
    private String ingestSecret = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getIngestSecret() { return ingestSecret; }
    public void setIngestSecret(String ingestSecret) { this.ingestSecret = ingestSecret; }

    public boolean isReady() {
        if (!enabled || ingestSecret == null || ingestSecret.isBlank()) {
            return false;
        }
        if (productCode == null || !productCode.matches("[a-z][a-z0-9-]*")) {
            return false;
        }
        return normalizedBaseUrl() != null;
    }

    public String normalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                return null;
            }
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
