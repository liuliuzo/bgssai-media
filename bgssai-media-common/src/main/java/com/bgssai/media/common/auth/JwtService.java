package com.bgssai.media.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private final com.bgssai.media.common.mapper.SysUserMapper users;
    private final SecretKey key;
    private final long expireMs;

    public JwtService(
            @Value("${bgssai.media.jwt.secret}") String secret,
            @Value("${bgssai.media.jwt.expire-ms}") long expireMs,
            com.bgssai.media.common.mapper.SysUserMapper users) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireMs;
        this.users = users;
    }

    public String createToken(AuthUser user) {
        String sid = java.util.UUID.randomUUID().toString();
        if (users.bindSession(user.getId(), user.getRole_code(), sid) != 1) throw new com.bgssai.media.common.web.BizException(401, "账号不可用");
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("sid", sid)
                .claim("username", user.getUsername())
                .claim("role_code", user.getRole_code())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public AuthUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String sid = claims.get("sid", String.class);
        String current = users.currentSession(Long.valueOf(claims.getSubject()), claims.get("role_code", String.class));
        if (sid == null || current == null) throw new com.bgssai.media.common.web.BizException(401, "登录已失效");
        if (!sid.equals(current)) throw new com.bgssai.media.common.web.BizException(2003, "账号已在其他设备登录");
        AuthUser user = new AuthUser();
        user.setId(Long.valueOf(claims.getSubject()));
        user.setUsername(claims.get("username", String.class));
        user.setRole_code(claims.get("role_code", String.class));
        return user;
    }
}
