package com.bgssai.media.user.controller;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.auth.AuthContext;
import com.bgssai.media.common.auth.RoleCodes;
import com.bgssai.media.common.oauth.OAuthUserInfo;
import com.bgssai.media.common.service.AccountSettingsService;
import com.bgssai.media.common.service.AuthService;
import com.bgssai.media.common.service.VerifyCodeService;
import com.bgssai.media.common.web.ApiResponse;
import com.bgssai.media.common.web.BizException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Account deletion and login-method bindings for mobile/desktop clients.
 * Paths: POST /api/account/delete, GET|POST|DELETE /api/account/bindings.
 */
@RestController
@RequestMapping("/api/account")
@NeedAop(roles = {RoleCodes.USER})
public class AccountController {

    private static final String CONSEQUENCE =
            "注销后将无法再用任何方式登录原账号；个人资料将匿名化；未结事项需先处理，不会静默清除。";

    private final AccountSettingsService accountSettingsService;
    private final VerifyCodeService verifyCodeService;
    private final AuthService authService;

    public AccountController(AccountSettingsService accountSettingsService,
                             VerifyCodeService verifyCodeService,
                             AuthService authService) {
        this.accountSettingsService = accountSettingsService;
        this.verifyCodeService = verifyCodeService;
        this.authService = authService;
    }

    @GetMapping("/deletion-notice")
    public ApiResponse<Map<String, Object>> deletionNotice() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("consequence", CONSEQUENCE);
        out.put("requires_verification", true);
        return ApiResponse.ok(out);
    }

    @PostMapping("/delete")
    public ApiResponse<Map<String, Object>> delete(@RequestBody Map<String, Object> body) {
        Long userId = AuthContext.get().getId();
        boolean ack = Boolean.TRUE.equals(body.get("acknowledged"))
                || Boolean.TRUE.equals(body.get("ack"))
                || "true".equalsIgnoreCase(String.valueOf(body.getOrDefault("acknowledged", "false")));
        if (!ack) {
            throw new BizException(400, "必须阅读并确认注销后果");
        }
        String password = str(body, "password");
        String emailCode = str(body, "email_code");
        String phoneCode = str(body, "phone_code");
        boolean verified = false;
        if (password != null && !password.isBlank()) {
            accountSettingsService.verifyPassword(userId, password);
            verified = true;
        }
        if (emailCode != null && !emailCode.isBlank()) {
            var user = accountSettingsService.requireActive(userId);
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                throw new BizException(400, "账号未绑定邮箱");
            }
            verifyCodeService.consume(user.getEmail(), emailCode, "LOGIN");
            verified = true;
        }
        if (phoneCode != null && !phoneCode.isBlank()) {
            var user = accountSettingsService.requireActive(userId);
            if (user.getPhone() == null || user.getPhone().isBlank()) {
                throw new BizException(400, "账号未绑定手机");
            }
            verifyCodeService.consume(user.getPhone(), phoneCode, "LOGIN");
            verified = true;
        }
        if (!verified) {
            throw new BizException(400, "未验证不注销");
        }
        Map<String, Object> result = accountSettingsService.deleteAccount(userId);
        result.put("consequence", CONSEQUENCE);
        result.put("deleted", true);
        return ApiResponse.ok(result);
    }

    @GetMapping("/bindings")
    public ApiResponse<Map<String, Object>> bindings() {
        Map<String, Object> methods = accountSettingsService.listLoginMethods(AuthContext.get().getId());
        methods.put("bindings", toBindingsList(methods));
        return ApiResponse.ok(methods);
    }

    @PostMapping("/bindings")
    public ApiResponse<Map<String, Object>> bind(@RequestBody Map<String, String> body) {
        Long userId = AuthContext.get().getId();
        Map<String, String> b = body == null ? Map.of() : body;
        String type = norm(b.get("type") != null ? b.get("type") : b.get("provider"));
        if (type.isEmpty()) {
            throw new BizException(400, "type required");
        }
        return switch (type) {
            case "email" -> {
                String email = b.get("email");
                String code = b.get("code");
                if (email == null || code == null || code.isBlank()) {
                    throw new BizException(400, "email and code required");
                }
                verifyCodeService.consume(email, code, "LOGIN");
                yield ApiResponse.ok(accountSettingsService.bindEmail(userId, email));
            }
            case "phone" -> {
                String phone = b.get("phone");
                String code = b.get("code");
                if (phone == null || code == null || code.isBlank()) {
                    throw new BizException(400, "phone and code required");
                }
                verifyCodeService.consume(phone, code, "LOGIN");
                yield ApiResponse.ok(accountSettingsService.bindPhone(userId, phone));
            }
            default -> {
                // Never trust client-supplied open_id / provider_uid. Exchange code+state
                // with the same OAuth path used by login.
                String code = b.get("code");
                String state = b.get("state");
                if (code == null || code.isBlank() || state == null || state.isBlank()) {
                    throw new BizException(400, "code and state required");
                }
                OAuthUserInfo profile = authService.exchangeOauthProfile(type, code, state);
                yield ApiResponse.ok(accountSettingsService.bindIdentity(
                        userId, type, profile.getOpenId(), profile.getNickname()));
            }
        };
    }

    @DeleteMapping("/bindings")
    public ApiResponse<Map<String, Object>> unbind(@RequestBody(required = false) Map<String, String> body) {
        Long userId = AuthContext.get().getId();
        Map<String, String> b = body == null ? Map.of() : body;
        String type = norm(b.get("type") != null ? b.get("type") : b.get("provider"));
        if (type.isEmpty()) {
            throw new BizException(400, "type required");
        }
        String password = b.get("password");
        if (password != null && !password.isBlank()) {
            accountSettingsService.verifyPassword(userId, password);
        }
        return switch (type) {
            case "password" -> throw new BizException(400, "账号密码不可解绑");
            case "email" -> ApiResponse.ok(accountSettingsService.unbindEmail(userId));
            case "phone" -> ApiResponse.ok(accountSettingsService.unbindPhone(userId));
            default -> ApiResponse.ok(accountSettingsService.unbindIdentity(userId, type));
        };
    }

    @GetMapping("/notices")
    public ApiResponse<java.util.List<Map<String, Object>>> notices() {
        return ApiResponse.ok(accountSettingsService.listNotices(AuthContext.get().getId()));
    }

    private static java.util.List<Map<String, Object>> toBindingsList(Map<String, Object> methods) {
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        list.add(item("password", Boolean.TRUE.equals(methods.get("has_password")), null));
        list.add(item("email", Boolean.TRUE.equals(methods.get("email_bound")), String.valueOf(methods.get("email"))));
        list.add(item("phone", Boolean.TRUE.equals(methods.get("phone_bound")), String.valueOf(methods.get("phone"))));
        Object identities = methods.get("identities");
        if (identities instanceof java.util.List<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> m) {
                    list.add(item(String.valueOf(m.get("provider")).toLowerCase(Locale.ROOT),
                            true, String.valueOf(m.get("nickname"))));
                }
            }
        }
        return list;
    }

    private static Map<String, Object> item(String type, boolean bound, String display) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("bound", bound);
        m.put("display", display);
        m.put("unbindable", bound && !"password".equals(type));
        return m;
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }

    private static String norm(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
