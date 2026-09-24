package com.bgssai.media.common.service;

import com.bgssai.media.common.auth.AuthUser;
import com.bgssai.media.common.auth.JwtService;
import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.domain.SysUserExample;
import com.bgssai.media.common.domain.UserIdentity;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.mapper.UserIdentityMapper;
import com.bgssai.media.common.oauth.ChatOAuthService;
import com.bgssai.media.common.oauth.CnOAuthClient;
import com.bgssai.media.common.oauth.OAuthAuthorizeResult;
import com.bgssai.media.common.oauth.OAuthSettings;
import com.bgssai.media.common.oauth.OAuthStateStore;
import com.bgssai.media.common.oauth.OAuthUserInfo;
import com.bgssai.media.common.sms.SmsSendResult;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.security.AccountPassword;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    /** 验证码登录场景，对应 platform_sms_config.login_template_id。 */
    private static final String SCENE_LOGIN = "LOGIN";
    private static final Set<String> CN_PROVIDERS = Set.of("WECHAT", "DOUYIN", "BAIDU", "ALIPAY");
    private static final Set<String> CHAT_PROVIDERS = Set.of("CHAT", "BGSSAI");

    private final SysUserMapper sysUserMapper;
    private final UserIdentityMapper userIdentityMapper;
    private final JwtService jwtService;
    private final VerifyCodeService verifyCodeService;
    private final OAuthSettings oauthSettings;
    private final OAuthStateStore oauthStateStore;
    private final CnOAuthClient cnOAuthClient;
    private final ChatOAuthService chatOAuthService;
    private final AccountPassword passwordEncoder = new AccountPassword();

    public AuthService(SysUserMapper sysUserMapper, UserIdentityMapper userIdentityMapper,
                       JwtService jwtService, VerifyCodeService verifyCodeService,
                       OAuthSettings oauthSettings, OAuthStateStore oauthStateStore,
                       CnOAuthClient cnOAuthClient, ChatOAuthService chatOAuthService) {
        this.sysUserMapper = sysUserMapper;
        this.userIdentityMapper = userIdentityMapper;
        this.jwtService = jwtService;
        this.verifyCodeService = verifyCodeService;
        this.oauthSettings = oauthSettings;
        this.oauthStateStore = oauthStateStore;
        this.cnOAuthClient = cnOAuthClient;
        this.chatOAuthService = chatOAuthService;
    }

    public Map<String, Object> loginByPassword(String username, String password, String requiredRole) {
        SysUserExample example = new SysUserExample();
        example.createCriteria().andUsernameEqualTo(username).andStatusEqualTo("active");
        List<SysUser> users = sysUserMapper.selectByExample(example);
        if (users.isEmpty()) {
            throw new BizException(401, "invalid username or password");
        }
        SysUser user = users.get(0);
        if (requiredRole != null && !requiredRole.equals(user.getRoleCode())) {
            throw new BizException(403, "role not allowed");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(401, "invalid username or password");
        }
        if (AccountPassword.isLegacy(user.getPasswordHash())) {
            SysUser patch = new SysUser();
            patch.setId(user.getId());
            patch.setPasswordHash(password);
            sysUserMapper.updateByPrimaryKeySelective(patch);
        }
        return tokenResult(user);
    }

    /**
     * 发邮箱验证码。
     *
     * <p>响应里<b>不带验证码</b>：把码回给调用方等于取消了「必须持有该邮箱」这个前提，
     * 任何能访问本接口的人都能登录任意已有用户。通道未配置时由发送器抛错，不静默成功。
     */
    public Map<String, Object> sendEmailOtp(String email) {
        verifyCodeService.sendEmailCode(email, SCENE_LOGIN);
        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        return result;
    }

    /**
     * 发手机验证码。同样不回显验证码；回 product + seq（序号）方便用户对照短信正文。
     */
    public Map<String, Object> sendPhoneOtp(String phone) {
        SmsSendResult sent = verifyCodeService.sendPhoneCode(phone, SCENE_LOGIN);
        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("product", sent.getProduct());
        result.put("seq", sent.getSeq());
        return result;
    }

    public Map<String, Object> loginByEmailOtp(String email, String code, String requiredRole) {
        verifyCodeService.consume(email, code, SCENE_LOGIN);
        SysUserExample example = new SysUserExample();
        example.createCriteria().andEmailEqualTo(email).andStatusEqualTo("active");
        List<SysUser> users = sysUserMapper.selectByExample(example);
        SysUser user = users.isEmpty()
                ? provisionUser("e" + Math.abs(email.hashCode()), email, null, requiredRole)
                : users.get(0);
        if (requiredRole != null && !requiredRole.equals(user.getRoleCode())) {
            throw new BizException(403, "role not allowed");
        }
        return tokenResult(user);
    }

    public Map<String, Object> loginByPhoneOtp(String phone, String code, String requiredRole) {
        verifyCodeService.consume(phone, code, SCENE_LOGIN);
        SysUserExample example = new SysUserExample();
        example.createCriteria().andPhoneEqualTo(phone).andStatusEqualTo("active");
        List<SysUser> users = sysUserMapper.selectByExample(example);
        SysUser user = users.isEmpty()
                ? provisionUser("p" + Math.abs(phone.hashCode()), null, phone, requiredRole)
                : users.get(0);
        if (requiredRole != null && !requiredRole.equals(user.getRoleCode())) {
            throw new BizException(403, "role not allowed");
        }
        return tokenResult(user);
    }

    /** 凭证已配齐的境内渠道。登录页据此决定画哪几个入口，而不是先画出来、点下去才报「未配置」。 */
    public java.util.List<String> configuredCnProviders() {
        java.util.List<String> open = new java.util.ArrayList<>();
        for (String provider : java.util.List.of("WECHAT", "DOUYIN", "BAIDU", "ALIPAY")) {
            if (oauthSettings.configured(provider)) {
                open.add(provider);
            }
        }
        return open;
    }

    public OAuthAuthorizeResult buildOauthAuthorize(String rawProvider) {
        String provider = rawProvider == null ? "" : rawProvider.trim().toUpperCase(Locale.ROOT);
        if (CHAT_PROVIDERS.contains(provider)) {
            return chatOAuthService.buildAuthorize();
        }
        if (!CN_PROVIDERS.contains(provider)) {
            throw new BizException(400, "unsupported login provider");
        }
        if (!oauthSettings.configured(provider)) {
            throw new BizException(400, "未配置该登录方式");
        }
        String state = oauthStateStore.issue(provider, null);
        OAuthAuthorizeResult result = new OAuthAuthorizeResult();
        result.setAuthorizeUrl(cnOAuthClient.buildAuthorizeUrl(provider, state));
        result.setState(state);
        return result;
    }

    public Map<String, Object> loginByOauth(String rawProvider, String requiredRole) {
        throw new BizException(400, oauthSettings.configured(rawProvider) ? "授权码不能为空" : "未配置该登录方式");
    }

    public Map<String, Object> completeOauth(String rawProvider, String code, String state, String requiredRole) {
        String provider = rawProvider == null ? "" : rawProvider.trim().toUpperCase(Locale.ROOT);
        String trimmedCode = code == null ? "" : code.trim();
        if (trimmedCode.isEmpty()) {
            throw new BizException(400, oauthSettings.configured(provider) ? "授权码不能为空" : "未配置该登录方式");
        }
        OAuthUserInfo profile;
        String storedProvider = provider;
        if (CHAT_PROVIDERS.contains(provider)) {
            profile = chatOAuthService.exchange(trimmedCode, state);
            storedProvider = "CHAT";
        } else if (CN_PROVIDERS.contains(provider)) {
            OAuthStateStore.Entry entry = oauthStateStore.consume(state);
            if (entry == null || !provider.equals(entry.provider)) {
                throw new BizException(400, "授权已过期，请重新发起");
            }
            profile = cnOAuthClient.exchange(provider, trimmedCode);
        } else {
            throw new BizException(400, "unsupported login provider");
        }
        if (profile == null || profile.getOpenId() == null || profile.getOpenId().isBlank()
                || "demo".equalsIgnoreCase(profile.getOpenId())) {
            throw new BizException(400, "未配置该登录方式");
        }
        UserIdentity existing = userIdentityMapper.findByProviderOpenId(storedProvider, profile.getOpenId());
        if (existing != null) {
            SysUser user = sysUserMapper.selectByPrimaryKey(existing.getUserId());
            if (user == null) {
                throw new BizException(401, "oauth user missing");
            }
            if (requiredRole != null && !requiredRole.equals(user.getRoleCode())) {
                throw new BizException(403, "role not allowed");
            }
            return tokenResult(user);
        }
        String username = storedProvider.toLowerCase(Locale.ROOT) + "_"
                + profile.getOpenId().replaceAll("[^a-zA-Z0-9]", "");
        if (username.length() > 40) {
            username = username.substring(0, 40);
        }
        SysUser user = provisionUser(username, null, null, requiredRole);
        UserIdentity identity = new UserIdentity();
        identity.setUserId(user.getId());
        identity.setProvider(storedProvider);
        identity.setOpenId(profile.getOpenId());
        identity.setNickname(profile.getNickname() == null ? user.getUsername() : profile.getNickname());
        userIdentityMapper.insert(identity);
        return tokenResult(user);
    }

    private SysUser provisionUser(String username, String email, String phone, String requiredRole) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoleCode(requiredRole == null ? "USER" : requiredRole);
        user.setStatus("active");
        sysUserMapper.insertSelective(user);
        return user;
    }

    public Map<String, Object> registerUser(String username, String password, String email, String phone) {
        SysUserExample example = new SysUserExample();
        example.createCriteria().andUsernameEqualTo(username);
        if (!sysUserMapper.selectByExample(example).isEmpty()) {
            throw new BizException(400, "username exists");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoleCode("USER");
        user.setStatus("active");
        sysUserMapper.insertSelective(user);
        return tokenResult(user);
    }

    private Map<String, Object> tokenResult(SysUser user) {
        AuthUser auth = new AuthUser(user.getId(), user.getUsername(), user.getRoleCode());
        String token = jwtService.createToken(auth);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user_id", user.getId());
        result.put("username", user.getUsername());
        result.put("role_code", user.getRoleCode());
        return result;
    }
}
