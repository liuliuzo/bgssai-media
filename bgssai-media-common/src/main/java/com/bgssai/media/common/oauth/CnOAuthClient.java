package com.bgssai.media.common.oauth;

import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
public class CnOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(CnOAuthClient.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OAuthSettings settings;
    private final OAuthHttp http;
    private final ObjectMapper mapper = new ObjectMapper();

    public CnOAuthClient(OAuthSettings settings, OAuthHttp http) {
        this.settings = settings;
        this.http = http;
    }

    public String buildAuthorizeUrl(String provider, String state) {
        String name = OAuthSettings.normalize(provider);
        if (!settings.configured(name)) {
            throw new BizException(400, "未配置该登录方式");
        }
        String encState = OAuthHttp.enc(state);
        return switch (name) {
            case "WECHAT" -> "https://open.weixin.qq.com/connect/qrconnect?appid=" + OAuthHttp.enc(settings.getWechatAppId())
                    + "&redirect_uri=" + OAuthHttp.enc(settings.getWechatRedirectUri())
                    + "&response_type=code&scope=snsapi_login&state=" + encState + "#wechat_redirect";
            case "DOUYIN" -> "https://open.douyin.com/platform/oauth/connect?client_key=" + OAuthHttp.enc(settings.getDouyinClientKey())
                    + "&response_type=code&scope=user_info&redirect_uri=" + OAuthHttp.enc(settings.getDouyinRedirectUri())
                    + "&state=" + encState;
            case "BAIDU" -> "https://openapi.baidu.com/oauth/2.0/authorize?client_id=" + OAuthHttp.enc(settings.getBaiduClientId())
                    + "&response_type=code&redirect_uri=" + OAuthHttp.enc(settings.getBaiduRedirectUri())
                    + "&scope=basic&display=popup&state=" + encState;
            case "ALIPAY" -> "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id=" + OAuthHttp.enc(settings.getAlipayAppId())
                    + "&scope=auth_user&redirect_uri=" + OAuthHttp.enc(settings.getAlipayRedirectUri())
                    + "&state=" + encState;
            default -> throw new BizException(400, "unsupported login provider");
        };
    }

    public OAuthUserInfo exchange(String provider, String code) {
        String name = OAuthSettings.normalize(provider);
        if (!settings.configured(name)) {
            throw new BizException(400, "未配置该登录方式");
        }
        try {
            return switch (name) {
                case "WECHAT" -> exchangeWechat(code);
                case "DOUYIN" -> exchangeDouyin(code);
                case "BAIDU" -> exchangeBaidu(code);
                case "ALIPAY" -> exchangeAlipay(code);
                default -> throw new BizException(400, "unsupported login provider");
            };
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("oauth exchange failed provider={} err={}", name, e.getClass().getSimpleName());
            throw new BizException(502, "授权失败，请稍后重试");
        }
    }

    private OAuthUserInfo exchangeWechat(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + OAuthHttp.enc(settings.getWechatAppId())
                + "&secret=" + OAuthHttp.enc(settings.getWechatAppSecret())
                + "&code=" + OAuthHttp.enc(code) + "&grant_type=authorization_code";
        JsonNode body = http.get(url);
        String openId = OAuthHttp.text(body, "openid");
        if (openId.isEmpty() || body.has("errcode")) {
            throw new BizException(502, "授权失败，请稍后重试");
        }
        String nick = OAuthHttp.text(body, "nickname");
        return new OAuthUserInfo(openId, nick.isEmpty() ? "微信用户" : nick);
    }

    private OAuthUserInfo exchangeDouyin(String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_key", settings.getDouyinClientKey());
        form.put("client_secret", settings.getDouyinClientSecret());
        form.put("code", code);
        form.put("grant_type", "authorization_code");
        JsonNode wrap = http.postForm("https://open.douyin.com/oauth/access_token/", form);
        JsonNode data = wrap.has("data") ? wrap.get("data") : wrap;
        String openId = OAuthHttp.text(data, "open_id");
        if (openId.isEmpty()) {
            throw new BizException(502, "授权失败，请稍后重试");
        }
        String nick = OAuthHttp.text(data, "nickname");
        return new OAuthUserInfo(openId, nick.isEmpty() ? "抖音用户" : nick);
    }

    private OAuthUserInfo exchangeBaidu(String code) {
        String tokenUrl = "https://openapi.baidu.com/oauth/2.0/token?grant_type=authorization_code&code=" + OAuthHttp.enc(code)
                + "&client_id=" + OAuthHttp.enc(settings.getBaiduClientId())
                + "&client_secret=" + OAuthHttp.enc(settings.getBaiduClientSecret())
                + "&redirect_uri=" + OAuthHttp.enc(settings.getBaiduRedirectUri());
        JsonNode token = http.get(tokenUrl);
        String access = OAuthHttp.text(token, "access_token");
        if (access.isEmpty()) {
            throw new BizException(502, "授权失败，请稍后重试");
        }
        JsonNode user = http.get("https://openapi.baidu.com/rest/2.0/passport/users/getLoggedInUser?access_token="
                + OAuthHttp.enc(access) + "&get_unionid=1");
        String openId = OAuthHttp.text(user, "openid");
        if (openId.isEmpty()) {
            openId = OAuthHttp.text(user, "userid");
        }
        if (openId.isEmpty()) {
            throw new BizException(502, "授权失败，请稍后重试");
        }
        String nick = OAuthHttp.text(user, "username");
        return new OAuthUserInfo(openId, nick.isEmpty() ? "百度用户" : nick);
    }

    private OAuthUserInfo exchangeAlipay(String code) throws Exception {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", settings.getAlipayAppId());
        params.put("method", "alipay.system.oauth.token");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TS));
        params.put("version", "1.0");
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("sign", rsa2Sign(params, settings.getAlipayAppPrivateKey()));
        String raw = http.postFormRaw("https://openapi.alipay.com/gateway.do", params);
        JsonNode root = mapper.readTree(raw);
        JsonNode payload = root.get("alipay_system_oauth_token_response");
        String openId = OAuthHttp.text(payload, "user_id");
        if (openId.isEmpty()) {
            openId = OAuthHttp.text(payload, "open_id");
        }
        if (openId.isEmpty()) {
            throw new BizException(502, "授权失败，请稍后重试");
        }
        return new OAuthUserInfo(openId, "支付宝用户");
    }

    static String rsa2Sign(TreeMap<String, String> params, String pem) throws Exception {
        StringBuilder content = new StringBuilder();
        params.forEach((k, v) -> {
            if (v == null || v.isEmpty() || "sign".equals(k)) {
                return;
            }
            if (content.length() > 0) {
                content.append('&');
            }
            content.append(k).append('=').append(v);
        });
        String cleaned = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        PrivateKey key = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(cleaned)));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(content.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
