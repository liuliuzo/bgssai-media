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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    public AuthService(SysUserMapper sysUserMapper, JwtService jwtService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtService = jwtService;
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

    public Map<String, Object> sendEmailOtp(String email) {
        String code = "123456";
        otpStore.put("email:" + email, code);
        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("stub_code", code);
        result.put("message", "MVP stub: OTP not actually emailed");
        return result;
    }

    public Map<String, Object> sendPhoneOtp(String phone) {
        String code = "123456";
        otpStore.put("phone:" + phone, code);
        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("stub_code", code);
        result.put("message", "MVP stub: OTP not actually SMS-sent");
        return result;
    }

    public Map<String, Object> loginByEmailOtp(String email, String code, String requiredRole) {
        String expect = otpStore.get("email:" + email);
        if (expect == null || !expect.equals(code)) {
            throw new BizException(401, "invalid email otp");
        }
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
        String expect = otpStore.get("phone:" + phone);
        if (expect == null || !expect.equals(code)) {
            throw new BizException(401, "invalid phone otp");
        }
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
