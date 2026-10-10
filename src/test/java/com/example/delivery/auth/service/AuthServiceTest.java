package com.example.delivery.auth.service;

import com.example.delivery.auth.dto.LoginRequest;
import com.example.delivery.auth.dto.LoginResult;
import com.example.delivery.auth.entity.RefreshToken;
import com.example.delivery.auth.repository.RefreshTokenRepository;
import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.security.JwtProvider;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import com.example.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @InjectMocks
    AuthService authService;

    private User user(UserRole role) {
        User user = User.builder()
                .loginId("user01")
                .password("encodedPassword")
                .role(role)
                .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        return user;
    }

    @Test
    @DisplayName("올바른 아이디와 비밀번호로 처음 로그인하면, 회원의 아이디와 역할로 Access/Refresh Token을 발급한다")
    void login_success() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 20, 12, 0);
        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(user(UserRole.OWNER)));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.createAccessToken("user01", UserRole.OWNER)).willReturn("access-token");
        given(jwtProvider.createRefreshToken("user01", UserRole.OWNER)).willReturn("refresh-token");
        given(jwtProvider.getExpiration("refresh-token")).willReturn(expiresAt);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        // when
        LoginResult result = authService.login(new LoginRequest("user01", "password123"));

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getToken()).isEqualTo("refresh-token");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("이미 Refresh Token이 저장된 회원이 다시 로그인하면, 기존 토큰을 새 토큰과 만료 시각으로 교체한다")
    void login_rotatesExistingRefreshToken() {
        // given
        LocalDateTime newExpiresAt = LocalDateTime.of(2026, 10, 20, 12, 0);
        RefreshToken existing = RefreshToken.create(1L, "old-refresh-token", LocalDateTime.of(2026, 10, 1, 12, 0));

        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(user(UserRole.OWNER)));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.createAccessToken("user01", UserRole.OWNER)).willReturn("access-token");
        given(jwtProvider.createRefreshToken("user01", UserRole.OWNER)).willReturn("new-refresh-token");
        given(jwtProvider.getExpiration("new-refresh-token")).willReturn(newExpiresAt);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(existing));

        // when
        authService.login(new LoginRequest("user01", "password123"));

        // then
        assertThat(existing.getToken()).isEqualTo("new-refresh-token");
        assertThat(existing.getExpiresAt()).isEqualTo(newExpiresAt);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 로그인하면, INVALID_CREDENTIALS 예외가 발생하고 토큰은 발급되지 않는다")
    void login_unknownLoginId() {
        // given
        given(userRepository.findByLoginId("unknown")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(jwtProvider, never()).createAccessToken(any(), any());
        verify(jwtProvider, never()).createRefreshToken(any(), any());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면, INVALID_CREDENTIALS 예외가 발생하고 토큰은 발급되지 않는다")
    void login_wrongPassword() {
        // given
        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(user(UserRole.CUSTOMER)));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "wrongPassword")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(jwtProvider, never()).createAccessToken(any(), any());
        verify(jwtProvider, never()).createRefreshToken(any(), any());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("탈퇴한 회원이 올바른 비밀번호로 로그인하면, DELETED_USER 예외가 발생하고 토큰은 발급되지 않는다")
    void login_deletedUser() {
        // given
        User deleted = user(UserRole.CUSTOMER);
        deleted.delete(1L);
        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(deleted));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DELETED_USER);

        verify(jwtProvider, never()).createAccessToken(any(), any());
        verify(jwtProvider, never()).createRefreshToken(any(), any());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("탈퇴한 회원이 틀린 비밀번호로 로그인하면, 탈퇴 사실이 드러나지 않도록 INVALID_CREDENTIALS 예외가 발생한다")
    void login_deletedUserWithWrongPassword() {
        // given
        User deleted = user(UserRole.CUSTOMER);
        deleted.delete(1L);
        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(deleted));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "wrongPassword")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
