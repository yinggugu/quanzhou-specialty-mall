package com.quanzhou.mall.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - Token 生成与验证 (jjwt 0.11.x + Java 8 兼容版)
 *
 * ✅ 双表拆分改造：用 type 字段（"user"/"admin"）替代旧 role 字段区分身份
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, SignatureAlgorithm.HS256.getJcaName());
    }

    /**
     * 生成 JWT Token（新签名：type 替代 role）
     * @param userId   用户ID（user表或admin表的ID）
     * @param username 用户名
     * @param type     身份类型："user" 或 "admin"
     */
    public String generateToken(Integer userId, String username, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", type); // ✅ 用 type 替代 role

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Token 中解析 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 Token 获取用户ID
     */
    public Integer getUserId(String token) {
        Claims claims = parseToken(token);
        Object obj = claims.get("userId");
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return null;
    }

    /**
     * 从 Token 获取用户名
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * ✅ 从 Token 获取身份类型："user" 或 "admin"
     */
    public String getType(String token) {
        Claims claims = parseToken(token);
        Object obj = claims.get("type");
        if (obj instanceof String) return (String) obj;
        return null;
    }

    /**
     * @deprecated 双表拆分后不再使用 role 字段，请使用 getType()
     */
    @Deprecated
    public Integer getRole(String token) {
        String type = getType(token);
        if ("admin".equals(type)) return 1;
        return 0; // user 或未知
    }
}
