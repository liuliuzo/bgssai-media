package com.bgssai.media.common.oauth;

import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthFailClosedTest {

    @Test
    void emptySettingsAreNotConfigured() {
        OAuthSettings settings = new OAuthSettings();
        assertFalse(settings.wechatConfigured());
        assertFalse(settings.chatConfigured());
    }

    @Test
    void cnAuthorizeFailsClosedWhenUnconfigured() {
        CnOAuthClient client = new CnOAuthClient(new OAuthSettings(), new OAuthHttp());
        BizException ex = assertThrows(BizException.class, () -> client.buildAuthorizeUrl("WECHAT", "st"));
        assertTrue(ex.getMessage().contains("未配置该登录方式"));
    }

    @Test
    void wechatAuthorizeUrlIsRealWhenConfigured() {
        OAuthSettings settings = new OAuthSettings();
        settings.putWechat("wx_media", "secret", "http://localhost:3002/login");
        String url = new CnOAuthClient(settings, new OAuthHttp()).buildAuthorizeUrl("WECHAT", "abc");
        assertTrue(url.startsWith("https://open.weixin.qq.com/connect/qrconnect"));
        assertTrue(url.contains("wx_media"));
    }

    @Test
    void chatAuthorizeUsesPkceWhenConfigured() {
        OAuthSettings settings = new OAuthSettings();
        settings.putChat("https://chat-cn.bgssai.com", "media-client", "", "http://localhost:3002/login");
        OAuthAuthorizeResult resp = new ChatOAuthService(settings, new OAuthHttp(), new OAuthStateStore()).buildAuthorize();
        assertTrue(resp.getAuthorizeUrl().contains("https://chat-cn.bgssai.com/oauth/authorize"));
        assertTrue(resp.getAuthorizeUrl().contains("code_challenge"));
        assertTrue(resp.getAuthorizeUrl().contains("S256"));
    }
}
