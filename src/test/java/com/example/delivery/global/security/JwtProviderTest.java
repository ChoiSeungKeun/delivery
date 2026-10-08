package com.example.delivery.global.security;

import com.example.delivery.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

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
    @DisplayName("Access Token을 발급하면, 토큰에서 로그인 ID와 역할을 꺼낼 수 있다")
    void createAccessToken_containsLoginIdAndRole() {
        // given
        JwtProvider provider = provider();

        // when
        String token = provider.createAccessToken("tester", UserRole.OWNER);

        // then
        assertThat(provider.getLoginId(token)).isEqualTo("tester");
        assertThat(provider.getRole(token)).isEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("토큰을 발급하면, payload에는 sub, role, type, exp만 담기고 비밀번호 같은 민감 정보는 없다")
    void createToken_containsOnlyAllowedClaims() {
        // given
        JwtProvider provider = provider();

        // when
        String token = provider.createAccessToken("tester", UserRole.CUSTOMER);

        // then
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "role", "type", "exp");
    }

    @Test
    @DisplayName("Access Token은 Access 검증만 통과하고, Refresh 검증은 통과하지 못한다")
    void validate_accessToken() {
        // given
        JwtProvider provider = provider();
        String token = provider.createAccessToken("tester", UserRole.CUSTOMER);

        // when & then
        assertThat(provider.validateAccessToken(token)).isTrue();
        assertThat(provider.validateRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token은 Refresh 검증만 통과하고, Access 검증은 통과하지 못한다")
    void validate_refreshToken() {
        // given
        JwtProvider provider = provider();
        String token = provider.createRefreshToken("tester", UserRole.CUSTOMER);

        // when & then
        assertThat(provider.validateRefreshToken(token)).isTrue();
        assertThat(provider.validateAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void validate_expiredToken() {
        // given: 만료 시간이 이미 지난 상태로 발급되도록 음수 기간을 설정
        JwtProvider provider = provider(SECRET, Duration.ofSeconds(-1), Duration.ofSeconds(-1));
        String accessToken = provider.createAccessToken("tester", UserRole.CUSTOMER);
        String refreshToken = provider.createRefreshToken("tester", UserRole.CUSTOMER);

        // when & then
        assertThat(provider.validateAccessToken(accessToken)).isFalse();
        assertThat(provider.validateRefreshToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 검증에 실패한다")
    void validate_tokenSignedWithOtherKey() {
        // given
        String token = provider(OTHER_SECRET, Duration.ofMinutes(30), Duration.ofDays(14))
                .createAccessToken("tester", UserRole.CUSTOMER);

        // when
        boolean valid = provider().validateAccessToken(token);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이나 빈 값은 예외 없이 검증에 실패한다")
    void validate_malformedToken() {
        // given
        JwtProvider provider = provider();

        // when & then
        assertThat(provider.validateAccessToken("not-a-jwt")).isFalse();
        assertThat(provider.validateAccessToken("")).isFalse();
        assertThat(provider.validateRefreshToken(null)).isFalse();
    }
}
