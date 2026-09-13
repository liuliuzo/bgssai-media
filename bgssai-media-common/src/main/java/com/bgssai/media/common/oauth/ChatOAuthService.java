package com.bgssai.media.common.oauth;

import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ChatOAuthService {

    private static final Logger log = LoggerFactory.getLogger(ChatOAuthService.class);

    private final OAuthSettings settings;
    private final OAuthHttp http;
    private final OAuthStateStore stateStore;

    public ChatOAuthService(OAuthSettings settings, OAuthHttp http, OAuthStateStore stateStore) {
        this.settings = settings;
        this.http = http;
        this.stateStore = stateStore;
    }

    public OAuthAuthorizeResult buildAuthorize() {
        if (!settings.chatConfigured()) {
            throw new BizException(400, "未配置该登录方式");
        }
        String verifier = OAuthStateStore.randomToken(32);
        String state = stateStore.issue("CHAT", verifier);
        String challenge = s256(verifier);
        String url = settings.chatIssuer() + "/oauth/authorize?response_type=code"
                + "&client_id=" + OAuthHttp.enc(settings.getChatClientId())
                + "&redirect_uri=" + OAuthHttp.enc(settings.getChatRedirectUri())
                + "&scope=" + OAuthHttp.enc("openid profile")
                + "&state=" + OAuthHttp.enc(state)
                + "&code_challenge=" + OAuthHttp.enc(challenge)
                + "&code_challenge_method=S256";
        OAuthAuthorizeResult resp = new OAuthAuthorizeResult();
        resp.setAuthorizeUrl(url);
        resp.setState(state);
        return resp;
    }

    public OAuthUserInfo exchange(String code, String state) {
        OAuthStateStore.Entry entry = stateStore.consume(state);
        if (entry == null || (!"CHAT".equals(entry.provider) && !"BGSSAI".equals(entry.provider))) {
            throw new BizException(400, "授权已过期，请重新发起");
        }
        if (!settings.chatConfigured()) {
            throw new BizException(400, "未配置该登录方式");
        }
        try {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("grant_type", "authorization_code");
            form.put("code", code);
            form.put("redirect_uri", settings.getChatRedirectUri());
            form.put("client_id", settings.getChatClientId());
            form.put("code_verifier", entry.codeVerifier);
            if (!settings.getChatClientSecret().isEmpty()) {
                form.put("client_secret", settings.getChatClientSecret());
            }
            JsonNode tokens = http.postForm(settings.chatIssuer() + "/oauth2/v1/token", form);
            String access = OAuthHttp.text(tokens, "access_token");
            if (access.isEmpty()) {
                throw new BizException(502, "Chat 授权失败，请稍后重试");
            }
            JsonNode profile = http.getBearer(settings.chatIssuer() + "/oauth2/v1/userinfo", access);
            String sub = OAuthHttp.text(profile, "sub");
            if (sub.isEmpty()) {
                throw new BizException(502, "Chat 授权失败，请稍后重试");
            }
            String nick = OAuthHttp.text(profile, "nickname");
            if (nick.isEmpty()) {
                nick = OAuthHttp.text(profile, "name");
            }
            if (nick.isEmpty()) {
                nick = "Chat 用户";
            }
            return new OAuthUserInfo(sub, nick);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("chat oauth exchange failed err={}", e.getClass().getSimpleName());
            throw new BizException(502, "Chat 授权失败，请稍后重试");
        }
    }

    static String s256(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
