package com.example.delivery.global.security;

import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.exception.JwtAuthenticationException;
import com.example.delivery.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private void assertFailsWith(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(JwtAuthenticationException.class)
                .extracting(e -> ((JwtAuthenticationException) e).getErrorCode())
                .isEqualTo(expected);
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
    @DisplayName("Access Token은 Access 검증만 통과하고, Refresh 검증은 INVALID_TOKEN으로 실패한다")
    void verify_accessToken() {
        // given
        JwtProvider provider = provider();
        String token = provider.createAccessToken("tester", UserRole.CUSTOMER);

        // when & then
        assertThatCode(() -> provider.verifyAccessToken(token)).doesNotThrowAnyException();
        assertFailsWith(() -> provider.verifyRefreshToken(token), ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token은 Refresh 검증만 통과하고, Access 검증은 INVALID_TOKEN으로 실패한다")
    void verify_refreshToken() {
        // given
        JwtProvider provider = provider();
        String token = provider.createRefreshToken("tester", UserRole.CUSTOMER);

        // when & then
        assertThatCode(() -> provider.verifyRefreshToken(token)).doesNotThrowAnyException();
        assertFailsWith(() -> provider.verifyAccessToken(token), ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("만료된 토큰은 EXPIRED_TOKEN으로 검증에 실패한다")
    void verify_expiredToken() {
        // given
        JwtProvider provider = provider(SECRET, Duration.ofSeconds(-1), Duration.ofSeconds(-1));
        String accessToken = provider.createAccessToken("tester", UserRole.CUSTOMER);
        String refreshToken = provider.createRefreshToken("tester", UserRole.CUSTOMER);

        // when & then
        assertFailsWith(() -> provider.verifyAccessToken(accessToken), ErrorCode.EXPIRED_TOKEN);
        assertFailsWith(() -> provider.verifyRefreshToken(refreshToken), ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 INVALID_TOKEN으로 검증에 실패한다")
    void verify_tokenSignedWithOtherKey() {
        // given
        String token = provider(OTHER_SECRET, Duration.ofMinutes(30), Duration.ofDays(14))
                .createAccessToken("tester", UserRole.CUSTOMER);

        // when & then
        assertFailsWith(() -> provider().verifyAccessToken(token), ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이나 빈 값은 INVALID_TOKEN으로 검증에 실패한다")
    void verify_malformedToken() {
        // given
        JwtProvider provider = provider();

        // when & then
        assertFailsWith(() -> provider.verifyAccessToken("not-a-jwt"), ErrorCode.INVALID_TOKEN);
        assertFailsWith(() -> provider.verifyAccessToken(""), ErrorCode.INVALID_TOKEN);
        assertFailsWith(() -> provider.verifyRefreshToken(null), ErrorCode.INVALID_TOKEN);
    }
}
