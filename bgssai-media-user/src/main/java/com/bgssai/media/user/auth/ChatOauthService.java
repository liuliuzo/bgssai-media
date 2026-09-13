package com.bgssai.media.user.auth;

import com.bgssai.media.common.web.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User-side Chat third-party login prep. Admin must not call this.
 *
 * <p>PREP (default): document authorize URL; never issue a local session.
 * Live token exchange stays fail-closed until Chat client secret is configured
 * outside git and a real exchange is wired.
 */
@Service
public class ChatOauthService {

    public static final String PROVIDER = "CHAT";

    private final boolean enabled;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String userinfoUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public ChatOauthService(
            @Value("${bgssai.media.chat.oauth.enabled:false}") boolean enabled,
            @Value("${bgssai.media.chat.oauth.authorize-url:}") String authorizeUrl,
            @Value("${bgssai.media.chat.oauth.token-url:}") String tokenUrl,
            @Value("${bgssai.media.chat.oauth.userinfo-url:}") String userinfoUrl,
            @Value("${bgssai.media.chat.oauth.client-id:}") String clientId,
            @Value("${bgssai.media.chat.oauth.client-secret:}") String clientSecret,
            @Value("${bgssai.media.chat.oauth.redirect-uri:}") String redirectUri,
            @Value("${bgssai.media.chat.oauth.scope:openid profile}") String scope) {
        this.enabled = enabled;
        this.authorizeUrl = trim(authorizeUrl);
        this.tokenUrl = trim(tokenUrl);
        this.userinfoUrl = trim(userinfoUrl);
        this.clientId = trim(clientId);
        this.clientSecret = trim(clientSecret);
        this.redirectUri = trim(redirectUri);
        this.scope = trim(scope).isEmpty() ? "openid profile" : trim(scope);
    }

    public Map<String, Object> prepare() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", PROVIDER);
        result.put("admin_supported", false);
        result.put("enabled", enabled);
        result.put("status", liveReady() ? "READY" : "PREP");
        result.put("client_id", clientId.isEmpty() ? null : clientId);
        result.put("redirect_uri", redirectUri.isEmpty() ? null : redirectUri);
        result.put("scope", scope);
        result.put("authorize_url_template", authorizeUrl.isEmpty() ? null : authorizeUrl);
        result.put("token_url", tokenUrl.isEmpty() ? null : tokenUrl);
        result.put("userinfo_url", userinfoUrl.isEmpty() ? null : userinfoUrl);
        result.put("secret_configured", !clientSecret.isEmpty());
        if (liveReady()) {
            String state = UUID.randomUUID().toString();
            result.put("state", state);
            result.put("authorize_url", buildAuthorizeUrl(state));
            result.put("message", "Chat authorize URL assembled; complete the Chat consent then callback");
        } else {
            result.put("state", null);
            result.put("authorize_url", null);
            result.put("message", "Chat third-party login is reserved on the user app; "
                    + "token exchange is not live and no session is issued");
        }
        return result;
    }

    /**
     * Fail-closed. Missing code → 400. Unwired Chat → 503. Never mint a USER JWT here.
     */
    public Map<String, Object> complete(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "code required");
        }
        if (state != null && state.length() > 128) {
            throw new BizException(400, "state too long");
        }
        if (!enabled) {
            throw new BizException(503, "Chat third-party login is not enabled");
        }
        if (clientSecret.isEmpty() || tokenUrl.isEmpty() || clientId.isEmpty()) {
            throw new BizException(503, "Chat token exchange not configured");
        }
        throw new BizException(503, "Chat token exchange is not live; user session not issued");
    }

    boolean liveReady() {
        return enabled
                && !authorizeUrl.isEmpty()
                && !tokenUrl.isEmpty()
                && !clientId.isEmpty()
                && !redirectUri.isEmpty()
                && !clientSecret.isEmpty();
    }

    private String buildAuthorizeUrl(String state) {
        return authorizeUrl
                + (authorizeUrl.contains("?") ? "&" : "?")
                + "response_type=code"
                + "&client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc(scope)
                + "&state=" + enc(state);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
