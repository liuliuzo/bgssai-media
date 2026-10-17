package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.domain.SysUserExample;
import com.bgssai.media.common.domain.UserIdentity;
import com.bgssai.media.common.mapper.OpcExtMapper;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.mapper.UserIdentityMapper;
import com.bgssai.media.common.security.AccountPassword;
import com.bgssai.media.common.util.SnakeMaps;
import com.bgssai.media.common.web.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Account settings (COMMON-016/017): login-method listing, bind / unbind of phone, email and
 * third-party identities, and self-service deletion.
 *
 * <p>OTP and password re-verification are done by the caller before these methods run.
 * Every unbind keeps at least one usable login method so the user cannot lock themselves out.
 */
@Service
public class AccountSettingsService {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DELETED = "deleted";
    public static final Set<String> BINDABLE_PROVIDERS = Set.of("WECHAT", "DOUYIN", "BAIDU", "ALIPAY", "CHAT");

    private final SysUserMapper sysUserMapper;
    private final UserIdentityMapper userIdentityMapper;
    private final OpcExtMapper opcExtMapper;
    private final AccountPassword passwordEncoder = new AccountPassword();

    public AccountSettingsService(SysUserMapper sysUserMapper,
                                  UserIdentityMapper userIdentityMapper,
                                  OpcExtMapper opcExtMapper) {
        this.sysUserMapper = sysUserMapper;
        this.userIdentityMapper = userIdentityMapper;
        this.opcExtMapper = opcExtMapper;
    }

    public SysUser requireActive(Long userId) {
        if (userId == null) {
            throw new BizException(401, "login required");
        }
        SysUser user = sysUserMapper.selectByPrimaryKey(userId);
        if (user == null || !STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BizException(401, "account not active");
        }
        return user;
    }

    public void verifyPassword(Long userId, String password) {
        SysUser user = requireActive(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(401, "密码不正确");
        }
    }

    public Map<String, Object> listLoginMethods(Long userId) {
        SysUser user = requireActive(userId);
        List<Map<String, Object>> identities = SnakeMaps.list(opcExtMapper.listIdentities(userId));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user_id", user.getId());
        out.put("username", user.getUsername());
        out.put("has_password", AccountPassword.isUsable(user.getPasswordHash()));
        out.put("email", maskEmail(user.getEmail()));
        out.put("email_bound", notBlank(user.getEmail()));
        out.put("phone", maskPhone(user.getPhone()));
        out.put("phone_bound", notBlank(user.getPhone()));
        List<Map<String, Object>> oauth = new ArrayList<>();
        for (Map<String, Object> row : identities) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("provider", row.get("provider"));
            m.put("nickname", row.get("nickname"));
            m.put("created_at", row.get("created_at"));
            oauth.add(m);
        }
        out.put("identities", oauth);
        out.put("method_count", countMethods(user));
        return out;
    }

    @Transactional
    public Map<String, Object> bindIdentity(Long userId, String rawProvider, String openId, String nickname) {
        SysUser user = requireActive(userId);
        String provider = normalizeProvider(rawProvider);
        if (openId == null || openId.isBlank()) {
            throw new BizException(400, "open_id required");
        }
        UserIdentity existing = userIdentityMapper.findByProviderOpenId(provider, openId);
        if (existing != null) {
            if (userId.equals(existing.getUserId())) {
                return listLoginMethods(userId);
            }
            throw new BizException(409, "该第三方账号已绑定其他用户");
        }
        for (Map<String, Object> row : opcExtMapper.listIdentities(userId)) {
            if (provider.equals(row.get("provider"))) {
                throw new BizException(409, "已绑定该登录方式，请先解绑");
            }
        }
        UserIdentity row = new UserIdentity();
        row.setUserId(user.getId());
        row.setProvider(provider);
        row.setOpenId(openId);
        row.setNickname(nickname == null || nickname.isBlank() ? user.getUsername() : nickname.trim());
        userIdentityMapper.insert(row);
        return listLoginMethods(userId);
    }

    @Transactional
    public Map<String, Object> unbindIdentity(Long userId, String rawProvider) {
        SysUser user = requireActive(userId);
        String provider = normalizeProvider(rawProvider);
        boolean bound = false;
        for (Map<String, Object> row : opcExtMapper.listIdentities(userId)) {
            if (provider.equals(row.get("provider"))) {
                bound = true;
                break;
            }
        }
        if (!bound) {
            throw new BizException(404, "未绑定该登录方式");
        }
        guardLastMethod(user);
        opcExtMapper.deleteIdentity(userId, provider);
        return listLoginMethods(userId);
    }

    @Transactional
    public Map<String, Object> bindPhone(Long userId, String phone) {
        SysUser user = requireActive(userId);
        String target = phone == null ? "" : phone.trim();
        if (!target.matches("1[3-9]\\d{9}")) {
            throw new BizException(400, "手机号格式不对");
        }
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andPhoneEqualTo(target).andStatusEqualTo(STATUS_ACTIVE);
        for (SysUser other : sysUserMapper.selectByExample(ex)) {
            if (!other.getId().equals(user.getId())) {
                throw new BizException(409, "该手机号已绑定其他用户");
            }
        }
        SysUser patch = new SysUser();
        patch.setId(user.getId());
        patch.setPhone(target);
        sysUserMapper.updateByPrimaryKeySelective(patch);
        return listLoginMethods(userId);
    }

    @Transactional
    public Map<String, Object> bindEmail(Long userId, String email) {
        SysUser user = requireActive(userId);
        String target = email == null ? "" : email.trim();
        if (!target.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new BizException(400, "邮箱格式不对");
        }
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andEmailEqualTo(target).andStatusEqualTo(STATUS_ACTIVE);
        for (SysUser other : sysUserMapper.selectByExample(ex)) {
            if (!other.getId().equals(user.getId())) {
                throw new BizException(409, "该邮箱已绑定其他用户");
            }
        }
        SysUser patch = new SysUser();
        patch.setId(user.getId());
        patch.setEmail(target);
        sysUserMapper.updateByPrimaryKeySelective(patch);
        return listLoginMethods(userId);
    }

    @Transactional
    public Map<String, Object> unbindPhone(Long userId) {
        SysUser user = requireActive(userId);
        if (!notBlank(user.getPhone())) {
            throw new BizException(404, "未绑定手机号");
        }
        guardLastMethod(user);
        sysUserMapper.clearPhone(userId);
        return listLoginMethods(userId);
    }

    @Transactional
    public Map<String, Object> unbindEmail(Long userId) {
        SysUser user = requireActive(userId);
        if (!notBlank(user.getEmail())) {
            throw new BizException(404, "未绑定邮箱");
        }
        guardLastMethod(user);
        sysUserMapper.clearEmail(userId);
        return listLoginMethods(userId);
    }

    /**
     * Soft-deletes the account: status=deleted, contact fields cleared, username anonymized,
     * password made unusable, session revoked, third-party identities released for re-use.
     */
    @Transactional
    public Map<String, Object> deleteAccount(Long userId) {
        SysUser user = requireActive(userId);
        Date now = new Date();
        String anonymized = "deleted_" + user.getId();
        int changed = opcExtMapper.softDeleteUser(user.getId(), anonymized, AccountPassword.unusable(), now);
        if (changed == 0) {
            throw new BizException(409, "account already deleted");
        }
        opcExtMapper.deleteAllIdentities(user.getId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user_id", user.getId());
        out.put("status", STATUS_DELETED);
        out.put("deleted_at", now);
        out.put("session_revoked", Boolean.TRUE);
        return out;
    }

    public List<Map<String, Object>> listNotices(Long userId) {
        requireActive(userId);
        return SnakeMaps.list(opcExtMapper.listNotices(userId, 100));
    }

    int countMethods(SysUser user) {
        int n = 0;
        if (AccountPassword.isUsable(user.getPasswordHash())) {
            n++;
        }
        if (notBlank(user.getEmail())) {
            n++;
        }
        if (notBlank(user.getPhone())) {
            n++;
        }
        n += opcExtMapper.countIdentities(user.getId());
        return n;
    }

    private void guardLastMethod(SysUser user) {
        if (countMethods(user) <= 1) {
            throw new BizException(409, "至少保留一种登录方式");
        }
    }

    private static String normalizeProvider(String raw) {
        String provider = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if ("BGSSAI".equals(provider)) {
            provider = "CHAT";
        }
        if (!BINDABLE_PROVIDERS.contains(provider)) {
            throw new BizException(400, "unsupported login provider");
        }
        return provider;
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    static String maskPhone(String phone) {
        if (!notBlank(phone)) {
            return null;
        }
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    static String maskEmail(String email) {
        if (!notBlank(email)) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
