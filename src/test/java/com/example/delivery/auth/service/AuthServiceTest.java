package com.example.delivery.auth.service;

import com.example.delivery.auth.dto.LoginRequest;
import com.example.delivery.auth.dto.LoginResult;
import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.security.JwtProvider;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import com.example.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @InjectMocks
    AuthService authService;

    private User user(UserRole role) {
        return User.builder()
                .loginId("user01")
                .password("encodedPassword")
                .role(role)
                .build();
    }

    @Test
    @DisplayName("올바른 아이디와 비밀번호로 로그인하면, 회원의 아이디와 역할로 Access/Refresh Token을 발급한다")
    void login_success() {
        // given
        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(user(UserRole.OWNER)));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.createAccessToken("user01", UserRole.OWNER)).willReturn("access-token");
        given(jwtProvider.createRefreshToken("user01", UserRole.OWNER)).willReturn("refresh-token");

        // when
        LoginResult result = authService.login(new LoginRequest("user01", "password123"));

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
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
