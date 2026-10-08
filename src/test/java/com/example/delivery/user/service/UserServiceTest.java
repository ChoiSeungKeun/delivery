package com.example.delivery.user.service;

import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.user.dto.UserJoinRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("중복되지 않은 로그인 ID로 회원가입 시, 비밀번호를 암호화하여 CUSTOMER 권한의 회원을 저장한다")
    void join_success() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "password123");
        given(userRepository.existsByLoginId("user01")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");

        // when
        userService.join(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getLoginId()).isEqualTo("user01");
        assertThat(saved.getPassword()).isEqualTo("encodedPassword");
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("회원가입 시, 원본 비밀번호는 저장되지 않고 암호화된 비밀번호만 저장된다")
    void join_passwordIsNotStoredAsPlainText() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "password123");
        given(userRepository.existsByLoginId("user01")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");

        // when
        userService.join(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isNotEqualTo("password123");
    }

    @Test
    @DisplayName("이미 가입된 로그인 ID로 회원가입 시, DUPLICATE_LOGIN_ID BusinessException이 발생하고 회원은 저장되지 않는다")
    void join_duplicateLoginId() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "password123");
        given(userRepository.existsByLoginId("user01")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.join(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

}