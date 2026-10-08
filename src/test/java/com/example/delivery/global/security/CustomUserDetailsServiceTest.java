package com.example.delivery.global.security;

import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import com.example.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService service;

    @Test
    @DisplayName("loginId로 회원을 찾으면, 아이디/비밀번호/권한이 담긴 UserDetails를 반환한다")
    void loadUser_success() {
        // given
        User user = User.builder().loginId("user01").password("encoded").role(UserRole.CUSTOMER).build();
        given(userRepository.findByLoginId("user01")).willReturn(Optional.of(user));

        // when
        UserDetails details = service.loadUserByUsername("user01");

        // then
        assertThat(details.getUsername()).isEqualTo("user01");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTOMER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loginId에 해당하는 회원이 없으면, UsernameNotFoundException이 발생한다")
    void loadUser_notFound() {
        // given
        given(userRepository.findByLoginId("ghost")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
