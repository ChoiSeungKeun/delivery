package com.example.delivery.user.controller;

import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.config.SecurityConfig;
import com.example.delivery.user.dto.UserJoinRequest;
import com.example.delivery.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String JOIN_URL = "/api/users/join";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    @DisplayName("올바른 요청으로 회원가입 시, 201과 성공 메시지를 반환하고 서비스에 가입을 요청한다")
    void join_success() throws Exception {
        // given
        String body = """
                { "loginId": "user01", "password": "password123" }
                """;

        // when
        ResultActions result = mockMvc.perform(post(JOIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());

        verify(userService).join(new UserJoinRequest("user01", "password123"));
    }

    @Test
    @DisplayName("로그인 ID 길이 검증에 실패하면, 400과 INVALID_INPUT 코드와 loginId 필드 오류를 반환하고 서비스는 호출되지 않는다")
    void join_invalidLoginId() throws Exception {
        // given
        String body = """
                { "loginId": "abc", "password": "password123" }
                """;

        // when
        ResultActions result = mockMvc.perform(post(JOIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("loginId"))
                .andExpect(jsonPath("$.errors[0].reason").value("로그인 ID는 4~20자여야 합니다."));

        verify(userService, never()).join(any());
    }

    @Test
    @DisplayName("비밀번호 길이 검증에 실패하면, 400과 INVALID_INPUT 코드와 password 필드 오류를 반환하고 서비스는 호출되지 않는다")
    void join_invalidPassword() throws Exception {
        // given
        String body = """
                { "loginId": "user01", "password": "pass123" }
                """;

        // when
        ResultActions result = mockMvc.perform(post(JOIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].reason").value("비밀번호는 8자 이상이어야 합니다."));

        verify(userService, never()).join(any());
    }

    @Test
    @DisplayName("로그인 ID와 비밀번호가 누락된 요청이면, 400과 두 필드의 오류를 반환하고 서비스는 호출되지 않는다")
    void join_missingFields() throws Exception {
        // given
        String body = "{}";

        // when
        ResultActions result = mockMvc.perform(post(JOIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'loginId')].reason").value("로그인 ID는 필수입니다."))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].reason").value("비밀번호는 필수입니다."));

        verify(userService, never()).join(any());
    }

    @Test
    @DisplayName("깨진 JSON 본문으로 요청하면, 400과 INVALID_INPUT 코드를 반환하고 서비스는 호출되지 않는다")
    void join_malformedJson() throws Exception {
        // given
        String body = "{ \"loginId\": \"user01\"";

        // when
        ResultActions result = mockMvc.perform(post(JOIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."));

        verify(userService, never()).join(any());
    }

    @Test
    @DisplayName("이미 가입된 로그인 ID로 요청하면, 서비스의 DUPLICATE_LOGIN_ID 예외가 409와 해당 에러코드 응답으로 변환된다")
    void join_duplicateLoginId() throws Exception {
        // given
        String body = """
                { "loginId": "user01", "password": "password123" }
                """;
        willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID))
                .given(userService).join(new UserJoinRequest("user01", "password123"));

        // when
        ResultActions result = mockMvc.perform(post(JOIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_LOGIN_ID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_LOGIN_ID.getMessage()));
    }
}
