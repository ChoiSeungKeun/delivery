package com.example.delivery.global.security;

import com.example.delivery.user.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = secretOf(1);
    private static final String OTHER_SECRET = secretOf(2);

    /** HS256 최소 길이(32바이트)를 채운 Base64 키 생성 */
    private static String secretOf(int fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private JwtProvider provider(String secret, Duration accessExp, Duration refreshExp) {
        return new JwtProvider(new JwtProperties(secret, accessExp, refreshExp));
    }

    private JwtProvider provider() {
        return provider(SECRET, Duration.ofMinutes(30), Duration.ofDays(14));
    }

    @Test
    @DisplayName("Access Token에서 로그인 ID와 역할을 꺼낼 수 있다")
    void accessToken_claims() {
        JwtProvider provider = provider();

        String token = provider.createAccessToken("tester", UserRole.OWNER);

        assertThat(provider.getLoginId(token)).isEqualTo("tester");
        assertThat(provider.getRole(token)).isEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("토큰 payload에는 sub, role, type, exp만 포함된다")
    void token_containsOnlyAllowedClaims() {
        JwtProvider provider = provider();
        String token = provider.createAccessToken("tester", UserRole.CUSTOMER);

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "role", "type", "exp");
    }

    @Test
    @DisplayName("Access Token은 Access 검증만 통과하고 Refresh 검증은 실패한다")
    void accessToken_isNotRefreshToken() {
        JwtProvider provider = provider();
        String token = provider.createAccessToken("tester", UserRole.CUSTOMER);

        assertThat(provider.validateAccessToken(token)).isTrue();
        assertThat(provider.validateRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token은 Refresh 검증만 통과하고 Access 검증은 실패한다")
    void refreshToken_isNotAccessToken() {
        JwtProvider provider = provider();
        String token = provider.createRefreshToken("tester", UserRole.CUSTOMER);

        assertThat(provider.validateRefreshToken(token)).isTrue();
        assertThat(provider.validateAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("type 클레임이 없는 토큰은 Access/Refresh 모두 검증에 실패한다")
    void tokenWithoutType_isInvalid() {
        JwtProvider provider = provider();
        String token = Jwts.builder()
                .subject("tester")
                .claim("role", "CUSTOMER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS256)
                .compact();

        assertThat(provider.validateAccessToken(token)).isFalse();
        assertThat(provider.validateRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void expiredToken_isInvalid() {
        JwtProvider provider = provider(SECRET, Duration.ofMillis(-1000), Duration.ofMillis(-1000));

        assertThat(provider.validateAccessToken(provider.createAccessToken("tester", UserRole.CUSTOMER))).isFalse();
        assertThat(provider.validateRefreshToken(provider.createRefreshToken("tester", UserRole.CUSTOMER))).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 검증에 실패한다")
    void tokenSignedWithOtherKey_isInvalid() {
        String token = provider(OTHER_SECRET, Duration.ofMinutes(30), Duration.ofDays(14))
                .createAccessToken("tester", UserRole.CUSTOMER);

        assertThat(provider().validateAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이나 빈 값은 예외 없이 검증에 실패한다")
    void malformedToken_isInvalid() {
        JwtProvider provider = provider();

        assertThat(provider.validateAccessToken("not-a-jwt")).isFalse();
        assertThat(provider.validateAccessToken("")).isFalse();
        assertThat(provider.validateRefreshToken(null)).isFalse();
    }
}
