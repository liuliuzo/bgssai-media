package com.bgssai.media.user.controller;

import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.AuthUser;
import com.bgssai.media.common.oauth.OAuthUserInfo;
import com.bgssai.media.common.service.AccountSettingsService;
import com.bgssai.media.common.service.AuthService;
import com.bgssai.media.common.service.VerifyCodeService;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountBindingsSecureTest {

    AccountSettingsService settings = mock(AccountSettingsService.class);
    VerifyCodeService verify = mock(VerifyCodeService.class);
    AuthService auth = mock(AuthService.class);
    AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(settings, verify, auth);
        AuthContext.set(new AuthUser(9L, "alice", "USER"));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void thirdPartyBindRejectsClientOpenIdWithoutCodeState() {
        BizException ex = assertThrows(BizException.class,
                () -> controller.bind(Map.of("type", "wechat", "open_id", "evil-open")));
        assertEquals(400, ex.getCode());
        verify(settings, never()).bindIdentity(anyLong(), anyString(), anyString(), anyString());
        verify(auth, never()).exchangeOauthProfile(anyString(), anyString(), anyString());
    }

    @Test
    void thirdPartyBindExchangesCodeStateThenBinds() {
        when(auth.exchangeOauthProfile("wechat", "auth-code", "st-1"))
                .thenReturn(new OAuthUserInfo("wx-real", "nick"));
        when(settings.bindIdentity(9L, "wechat", "wx-real", "nick"))
                .thenReturn(Map.of("bound", true));

        var resp = controller.bind(Map.of(
                "type", "wechat",
                "code", "auth-code",
                "state", "st-1",
                "open_id", "should-ignore"));
        assertEquals(true, resp.getResult().get("bound"));
        verify(auth).exchangeOauthProfile("wechat", "auth-code", "st-1");
        verify(settings).bindIdentity(eq(9L), eq("wechat"), eq("wx-real"), eq("nick"));
    }
}
