package com.bgssai.media.common.service;

import com.bgssai.media.common.auth.AuthUser;
import com.bgssai.media.common.auth.JwtService;
import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.domain.SysUserExample;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.web.BizException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    /** 验证码登录场景，对应 platform_sms_config.login_template_id。 */
    private static final String SCENE_LOGIN = "LOGIN";

    private final SysUserMapper sysUserMapper;
    private final JwtService jwtService;
    private final VerifyCodeService verifyCodeService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(SysUserMapper sysUserMapper, JwtService jwtService, VerifyCodeService verifyCodeService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtService = jwtService;
        this.verifyCodeService = verifyCodeService;
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

    /** 发手机验证码。同样不回显验证码。 */
    public Map<String, Object> sendPhoneOtp(String phone) {
        verifyCodeService.sendPhoneCode(phone, SCENE_LOGIN);
        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        return result;
    }

    public Map<String, Object> loginByEmailOtp(String email, String code, String requiredRole) {
        // consume 校验通过即把该码置为已用；同一个码不能登录第二次。
        verifyCodeService.consume(email, code, SCENE_LOGIN);
        SysUserExample example = new SysUserExample();
        example.createCriteria().andEmailEqualTo(email).andStatusEqualTo("active");
        List<SysUser> users = sysUserMapper.selectByExample(example);
        if (users.isEmpty()) {
            throw new BizException(401, "user not found");
        }
        SysUser user = users.get(0);
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
        if (users.isEmpty()) {
            throw new BizException(401, "user not found");
        }
        SysUser user = users.get(0);
        if (requiredRole != null && !requiredRole.equals(user.getRoleCode())) {
            throw new BizException(403, "role not allowed");
        }
        return tokenResult(user);
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
