package com.bgssai.media.user.auth;

import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatOauthServiceTest {

    @Test
    void defaultPrepDoesNotIssueAuthorizeUrlOrSecret() {
        ChatOauthService service = new ChatOauthService(
                false,
                "https://chat.bgssai.com/oauth/authorize",
                "https://chat.bgssai.com/oauth/token",
                "https://chat.bgssai.com/oauth/userinfo",
                "bgssai-media-user",
                "",
                "http://127.0.0.1:3002/login/chat/callback",
                "openid profile");
        Map<String, Object> prep = service.prepare();
        assertEquals("CHAT", prep.get("provider"));
        assertEquals(Boolean.FALSE, prep.get("admin_supported"));
        assertEquals("PREP", prep.get("status"));
        assertNull(prep.get("authorize_url"));
        assertEquals(Boolean.FALSE, prep.get("secret_configured"));
        assertFalse(String.valueOf(prep).toLowerCase().contains("secret-value"));
    }

    @Test
    void callbackWithoutCodeIs400() {
        ChatOauthService service = disabled();
        BizException ex = assertThrows(BizException.class, () -> service.complete("  ", "s"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void callbackWhenDisabledIs503AndIssuesNoSession() {
        ChatOauthService service = disabled();
        BizException ex = assertThrows(BizException.class, () -> service.complete("abc", "s"));
        assertEquals(503, ex.getCode());
        assertTrue(ex.getMessage().contains("not enabled"));
    }

    @Test
    void liveReadyStillDoesNotMintSession() {
        ChatOauthService service = new ChatOauthService(
                true,
                "https://chat.bgssai.com/oauth/authorize",
                "https://chat.bgssai.com/oauth/token",
                "https://chat.bgssai.com/oauth/userinfo",
                "bgssai-media-user",
                "not-a-real-secret",
                "http://127.0.0.1:3002/login/chat/callback",
                "openid profile");
        assertTrue(service.liveReady());
        Map<String, Object> prep = service.prepare();
        assertEquals("READY", prep.get("status"));
        assertTrue(String.valueOf(prep.get("authorize_url")).contains("response_type=code"));
        assertFalse(String.valueOf(prep).contains("not-a-real-secret"));
        BizException ex = assertThrows(BizException.class, () -> service.complete("abc", "s"));
        assertEquals(503, ex.getCode());
        assertTrue(ex.getMessage().contains("not live"));
    }

    private static ChatOauthService disabled() {
        return new ChatOauthService(false, "", "", "", "", "", "", "");
    }
}
