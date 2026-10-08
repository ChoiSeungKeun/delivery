package com.example.delivery.global.security;

import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtProvider jwtProvider;

    @Mock
    CustomUserDetailsService userDetailsService;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void doFilter(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        new JwtAuthenticationFilter(jwtProvider, userDetailsService).doFilter(request, response, chain);
    }

    @Test
    @DisplayName("유효한 Access Token이면, SecurityContext에 인증 정보가 등록된다")
    void validToken_setsAuthentication() throws Exception {
        // given
        User user = User.builder().loginId("user01").password("encoded").role(UserRole.OWNER).build();
        given(jwtProvider.validateAccessToken("token")).willReturn(true);
        given(jwtProvider.getLoginId("token")).willReturn("user01");
        given(userDetailsService.loadUserByUsername("user01")).willReturn(new CustomUserDetails(user));

        // when
        doFilter("Bearer token");

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user01");
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OWNER");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면, 인증 없이 다음 필터로 넘어간다")
    void noHeader_passesWithoutAuthentication() throws Exception {
        // given: Authorization 헤더가 없는 요청

        // when
        doFilter(null);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtProvider, userDetailsService);
    }

    @Test
    @DisplayName("검증에 실패한 토큰이면, 인증 없이 다음 필터로 넘어가고 회원 조회는 하지 않는다")
    void invalidToken_passesWithoutAuthentication() throws Exception {
        // given
        given(jwtProvider.validateAccessToken("invalid-token")).willReturn(false);

        // when
        doFilter("Bearer invalid-token");

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰이면, 인증 없이 다음 필터로 넘어간다")
    void deletedUser_passesWithoutAuthentication() throws Exception {
        // given
        User deleted = User.builder().loginId("user01").password("encoded").role(UserRole.OWNER).build();
        deleted.delete(1L);
        given(jwtProvider.validateAccessToken("valid-token")).willReturn(true);
        given(jwtProvider.getLoginId("valid-token")).willReturn("user01");
        given(userDetailsService.loadUserByUsername("user01")).willReturn(new CustomUserDetails(deleted));

        // when
        doFilter("Bearer valid-token");

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
