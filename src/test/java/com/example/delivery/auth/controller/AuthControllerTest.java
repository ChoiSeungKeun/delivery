package com.example.delivery.auth.controller;

import com.example.delivery.auth.dto.LoginRequest;
import com.example.delivery.auth.dto.LoginResult;
import com.example.delivery.auth.service.AuthService;
import com.example.delivery.global.config.SecurityConfig;
import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String LOGIN_URL = "/api/auth/login";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("올바른 요청으로 로그인하면, 인증 없이 200을 받고 Access Token은 body로, Refresh Token은 HttpOnly 쿠키로 전달된다")
    void login_success() throws Exception {
        // given
        String body = """
                { "loginId": "user01", "password": "password123" }
                """;
        given(authService.login(new LoginRequest("user01", "password123")))
                .willReturn(new LoginResult("access-token", "refresh-token"));
        given(jwtProvider.getRefreshTokenExpirationSeconds()).willReturn(1209600L);

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        String setCookie = result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith("refreshToken=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth")
                .contains("Max-Age=1209600");
    }

    @Test
    @DisplayName("아이디 또는 비밀번호가 올바르지 않으면, 401과 INVALID_CREDENTIALS 코드를 반환하고 쿠키는 내려가지 않는다")
    void login_invalidCredentials() throws Exception {
        // given
        String body = """
                { "loginId": "user01", "password": "wrongPassword" }
                """;
        given(authService.login(new LoginRequest("user01", "wrongPassword")))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("탈퇴한 회원이면, 403과 DELETED_USER 코드를 반환하고 쿠키는 내려가지 않는다")
    void login_deletedUser() throws Exception {
        // given
        String body = """
                { "loginId": "user01", "password": "password123" }
                """;
        given(authService.login(new LoginRequest("user01", "password123")))
                .willThrow(new BusinessException(ErrorCode.DELETED_USER));

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.DELETED_USER.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DELETED_USER.getMessage()))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("로그인 ID와 비밀번호가 누락된 요청이면, 400과 두 필드의 오류를 반환하고 서비스는 호출되지 않는다")
    void login_missingFields() throws Exception {
        // given
        String body = "{}";

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'loginId')].reason").value("로그인 ID는 필수입니다."))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].reason").value("비밀번호는 필수입니다."));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("깨진 JSON 본문으로 요청하면, 400과 INVALID_INPUT 코드를 반환하고 서비스는 호출되지 않는다")
    void login_malformedJson() throws Exception {
        // given
        String body = "{ \"loginId\": \"user01\"";

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));

        verify(authService, never()).login(any());
    }
}
