package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.McpToken;
import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.mapper.McpTokenMapper;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.web.BizException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * MCP PAT：对照 blog 口径——创建时回显明文一次，落库只存 SHA-256；
 * 列表只给 masked 展示字段。
 */
@Service
public class McpTokenService {

    public static final String TOKEN_PREFIX = "bm_mcp_";
    private static final int RANDOM_BYTES = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final McpTokenMapper mcpTokenMapper;
    private final SysUserMapper sysUserMapper;

    public McpTokenService(McpTokenMapper mcpTokenMapper, SysUserMapper sysUserMapper) {
        this.mcpTokenMapper = mcpTokenMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public List<Map<String, Object>> listForUser(Long userId) {
        List<McpToken> rows = mcpTokenMapper.selectActiveByUserId(userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (McpToken row : rows) {
            out.add(toPublicView(row));
        }
        return out;
    }

    public Map<String, Object> create(Long userId, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BizException(400, "token name required");
        }
        if (trimmed.length() > 64) {
            throw new BizException(400, "token name too long");
        }
        String plain = generatePlainToken();
        McpToken row = new McpToken();
        row.setUserId(userId);
        row.setName(trimmed);
        row.setTokenPrefix(plain.substring(0, Math.min(12, plain.length())));
        row.setTokenSuffix(plain.substring(Math.max(0, plain.length() - 4)));
        row.setTokenHash(sha256(plain));
        row.setStatus("active");
        mcpTokenMapper.insertSelective(row);

        Map<String, Object> result = toPublicView(row);
        result.put("token", plain);
        return result;
    }

    public void revoke(Long userId, Long tokenId) {
        McpToken existing = mcpTokenMapper.selectByIdAndUserId(tokenId, userId);
        if (existing == null) {
            throw new BizException(404, "token not found");
        }
        if (!"active".equals(existing.getStatus())) {
            return;
        }
        int updated = mcpTokenMapper.revoke(tokenId, userId, new Date());
        if (updated == 0) {
            throw new BizException(404, "token not found");
        }
    }

    /**
     * 用 Bearer 明文令牌解析所属用户；成功则刷新 last_used_at。
     * @return 用户，失败返回 null
     */
    public SysUser authenticate(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }
        String plain = bearerToken.trim();
        if (plain.regionMatches(true, 0, "Bearer ", 0, 7)) {
            plain = plain.substring(7).trim();
        }
        if (plain.isEmpty()) {
            return null;
        }
        McpToken row = mcpTokenMapper.selectActiveByHash(sha256(plain));
        if (row == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectByPrimaryKey(row.getUserId());
        if (user == null || !"active".equalsIgnoreCase(user.getStatus())) {
            return null;
        }
        mcpTokenMapper.touchLastUsed(row.getId(), new Date());
        return user;
    }

    static String generatePlainToken() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return TOKEN_PREFIX + HexFormat.of().formatHex(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    static String mask(String prefix, String suffix) {
        return (prefix == null ? "" : prefix) + "..." + (suffix == null ? "" : suffix);
    }

    private static Map<String, Object> toPublicView(McpToken row) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", row.getId());
        map.put("name", row.getName());
        map.put("token_masked", mask(row.getTokenPrefix(), row.getTokenSuffix()));
        map.put("status", row.getStatus());
        map.put("last_used_at", row.getLastUsedAt());
        map.put("created_at", row.getCreatedAt());
        return map;
    }
}
