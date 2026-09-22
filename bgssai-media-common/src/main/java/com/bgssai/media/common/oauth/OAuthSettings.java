package com.bgssai.media.common.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class OAuthSettings {

    @Value("${bgssai.oauth.wechat.app-id:}")
    private String wechatAppId;
    @Value("${bgssai.oauth.wechat.app-secret:}")
    private String wechatAppSecret;
    @Value("${bgssai.oauth.wechat.redirect-uri:}")
    private String wechatRedirectUri;

    @Value("${bgssai.oauth.douyin.client-key:}")
    private String douyinClientKey;
    @Value("${bgssai.oauth.douyin.client-secret:}")
    private String douyinClientSecret;
    @Value("${bgssai.oauth.douyin.redirect-uri:}")
    private String douyinRedirectUri;

    @Value("${bgssai.oauth.baidu.client-id:}")
    private String baiduClientId;
    @Value("${bgssai.oauth.baidu.client-secret:}")
    private String baiduClientSecret;
    @Value("${bgssai.oauth.baidu.redirect-uri:}")
    private String baiduRedirectUri;

    @Value("${bgssai.oauth.alipay.app-id:}")
    private String alipayAppId;
    @Value("${bgssai.oauth.alipay.app-private-key:}")
    private String alipayAppPrivateKey;
    @Value("${bgssai.oauth.alipay.alipay-public-key:}")
    private String alipayPublicKey;
    @Value("${bgssai.oauth.alipay.redirect-uri:}")
    private String alipayRedirectUri;

    @Value("${bgssai.chat.issuer:https://www.bgssai-chat.cn}")
    private String chatIssuer;
    @Value("${bgssai.chat.client-id:}")
    private String chatClientId;
    @Value("${bgssai.chat.client-secret:}")
    private String chatClientSecret;
    @Value("${bgssai.chat.redirect-uri:}")
    private String chatRedirectUri;

    public boolean wechatConfigured() { return filled(wechatAppId, wechatAppSecret, wechatRedirectUri); }
    public boolean douyinConfigured() { return filled(douyinClientKey, douyinClientSecret, douyinRedirectUri); }
    public boolean baiduConfigured() { return filled(baiduClientId, baiduClientSecret, baiduRedirectUri); }
    public boolean alipayConfigured() { return filled(alipayAppId, alipayAppPrivateKey, alipayRedirectUri); }
    public boolean chatConfigured() { return filled(chatIssuer(), chatClientId, chatRedirectUri); }

    public boolean configured(String provider) {
        String name = normalize(provider);
        return switch (name) {
            case "WECHAT" -> wechatConfigured();
            case "DOUYIN" -> douyinConfigured();
            case "BAIDU" -> baiduConfigured();
            case "ALIPAY" -> alipayConfigured();
            case "CHAT", "BGSSAI" -> chatConfigured();
            default -> false;
        };
    }

    public String chatIssuer() {
        return trimSlash(chatIssuer == null || chatIssuer.isBlank() ? "https://www.bgssai-chat.cn" : chatIssuer);
    }

    public String getWechatAppId() { return trim(wechatAppId); }
    public String getWechatAppSecret() { return trim(wechatAppSecret); }
    public String getWechatRedirectUri() { return trim(wechatRedirectUri); }
    public String getDouyinClientKey() { return trim(douyinClientKey); }
    public String getDouyinClientSecret() { return trim(douyinClientSecret); }
    public String getDouyinRedirectUri() { return trim(douyinRedirectUri); }
    public String getBaiduClientId() { return trim(baiduClientId); }
    public String getBaiduClientSecret() { return trim(baiduClientSecret); }
    public String getBaiduRedirectUri() { return trim(baiduRedirectUri); }
    public String getAlipayAppId() { return trim(alipayAppId); }
    public String getAlipayAppPrivateKey() { return trim(alipayAppPrivateKey); }
    public String getAlipayPublicKey() { return trim(alipayPublicKey); }
    public String getAlipayRedirectUri() { return trim(alipayRedirectUri); }
    public String getChatClientId() { return trim(chatClientId); }
    public String getChatClientSecret() { return trim(chatClientSecret); }
    public String getChatRedirectUri() { return trim(chatRedirectUri); }

    void putWechat(String appId, String secret, String redirect) {
        this.wechatAppId = appId;
        this.wechatAppSecret = secret;
        this.wechatRedirectUri = redirect;
    }

    void putChat(String issuer, String clientId, String secret, String redirect) {
        this.chatIssuer = issuer;
        this.chatClientId = clientId;
        this.chatClientSecret = secret;
        this.chatRedirectUri = redirect;
    }

    public static String normalize(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
    }

    static boolean filled(String... parts) {
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) return false;
        }
        return true;
    }

    static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    static String trimSlash(String url) {
        String value = trim(url);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
