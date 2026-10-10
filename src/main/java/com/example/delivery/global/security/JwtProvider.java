package com.example.delivery.global.security;

import com.example.delivery.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey key;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secretKey()));
        this.accessTokenExpiration = properties.accessTokenExpiration();
        this.refreshTokenExpiration = properties.refreshTokenExpiration();
    }

    public String createAccessToken(String loginId, UserRole role) {
        return createToken(loginId, role, TokenType.ACCESS, accessTokenExpiration.toMillis());
    }

    public String createRefreshToken(String loginId, UserRole role) {
        return createToken(loginId, role, TokenType.REFRESH, refreshTokenExpiration.toMillis());
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration.toSeconds();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, TokenType.ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, TokenType.REFRESH);
    }

    private boolean validateToken(String token, TokenType expectedType) {
        try {
            Claims claims = parse(token);
            return expectedType.name().equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getLoginId(String token) {
        return parse(token).getSubject();
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(parse(token).get(CLAIM_ROLE, String.class));
    }

    public LocalDateTime getExpiration(String token) {
        Date expiration = parse(token).getExpiration();

        return expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private String createToken(String loginId, UserRole role, TokenType type, long expirationMillis) {
        Date expiration = new Date(System.currentTimeMillis() + expirationMillis);

        return Jwts.builder()
                .subject(loginId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type.name())
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 서명(HS256)과 만료시각을 검증하고 Claims를 반환. 검증 실패 시 JwtException. */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
