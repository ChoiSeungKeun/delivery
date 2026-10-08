package com.example.delivery.global.exception;

import com.example.delivery.global.response.ApiResponse;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandlerTest.TestSecurityConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    // handleBusiness Test
    @Test
    @DisplayName("BusinessException(기본 메시지) 발생 시, ErrorCode의 HTTP 상태와 에러코드와 메시지를 담은 실패 응답을 반환한다")
    void handleBusiness_defaultMessage() throws Exception {
        // given
        String url = "/test/business";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FORBIDDEN.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("BusinessException(사용자 지정 메시지) 발생 시, 지정한 메시지를 담은 실패 응답을 반환한다")
    void handleBusiness_customMessage() throws Exception {
        // given
        String url = "/test/business/custom-message";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("사장님만 접근할 수 있습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    // handleValidation Test
    @Test
    @DisplayName("@Valid 필드 검증 실패 시, 400과 에러코드와 필드명과 메시지를 담은 실패 응답을 반환한다")
    void handleValidation_fieldErrors() throws Exception {
        // given
        String body = """
                { "name": "", "minOrderAmount": -1 }
                """;

        // when
        ResultActions result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].reason").value("이름은 필수입니다."))
                .andExpect(jsonPath("$.errors[?(@.field == 'minOrderAmount')].reason")
                        .value("최소 주문 금액은 0 이상이어야 합니다."));
    }

    @Test
    @DisplayName("@Valid 클래스 레벨 검증 실패 시, 객체 이름과 메시지를 errors에 담은 실패 응답을 반환한다")
    void handleValidation_globalErrors() throws Exception {
        // given
        String body = """
                { "start": 10, "end": 1 }
                """;

        // when
        ResultActions result = mockMvc.perform(post("/test/validation/global")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("rangeRequest"))
                .andExpect(jsonPath("$.errors[0].reason").value("시작값은 종료값보다 클 수 없습니다."));
    }

    // handleNotReadable Test
    @Test
    @DisplayName("깨진 JSON 본문 요청 시, 400과 INVALID_INPUT 코드와 고정 메시지를 담은 실패 응답을 반환한다")
    void handleNotReadable_response() throws Exception {
        // given
        String body = "{ \"name\": \"치킨\"";

        // when
        ResultActions result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("깨진 JSON 본문 요청 시, 로그에 경고를 기록하고 상세 정보를 응답에 노출하지 않는다")
    void handleNotReadable_logAndNoLeak(CapturedOutput output) throws Exception {
        // given
        String body = "{ \"name\": \"치킨\"";

        // when
        ResultActions result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        assertThat(output.getOut() + output.getErr()).contains("Unreadable request body");
        assertThat(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("JSON parse error")
                .doesNotContain("Unexpected");
    }

    // handleMissingParameter Test
    @Test
    @DisplayName("필수 요청 파라미터 누락 시, 400과 INVALID_INPUT 코드와 누락된 파라미터명을 담은 실패 응답을 반환한다")
    void handleMissingParameter() throws Exception {
        // given: keyword 파라미터를 필수로 받는 URL
        String url = "/test/param";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("keyword"))
                .andExpect(jsonPath("$.errors[0].reason").value("필수 파라미터입니다."));
    }

    // handleMethodNotAllowed Test
    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 요청 시, 405와 METHOD_NOT_ALLOWED 코드와 메시지를 담은 실패 응답을 반환한다")
    void handleMethodNotAllowed() throws Exception {
        // given: POST만 허용하는 URL
        String url = "/test/validation";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.METHOD_NOT_ALLOWED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());;
    }

    // handleDataIntegrity Test
    @Test
    @DisplayName("DataIntegrityViolationException 발생 시, 409와 DATA_CONFLICT 코드와 메시지를 담은 실패 응답을 반환한다")
    void handleDataIntegrity_response() throws Exception {
        // given
        String url = "/test/data-integrity";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.DATA_CONFLICT.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DATA_CONFLICT.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("DataIntegrityViolationException 발생 시, 로그에 원인을 기록하고 제약 조건 등 내부 정보를 응답에 노출하지 않는다")
    void handleDataIntegrity_logAndNoLeak(CapturedOutput output) throws Exception {
        // given
        String url = "/test/data-integrity";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        assertThat(output.getOut() + output.getErr())
                .contains("Data integrity violation")
                .contains("uk_user_login_id");
        assertThat(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("uk_user_login_id")
                .doesNotContain("duplicate key");
    }

    // handleUnexpected Test
    @Test
    @DisplayName("예상하지 못한 예외 발생 시, 500과 INTERNAL_SERVER_ERROR 코드를 담은 실패 응답을 반환한다")
    void handleUnexpected_response() throws Exception {
        // given
        String url = "/test/unexpected";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("예상하지 못한 예외 발생 시, 서버 로그에 예외를 기록하고 내부 정보를 응답에 노출하지 않는다")
    void handleUnexpected_logAndNoLeak(CapturedOutput output) throws Exception {
        // given
        String url = "/test/unexpected";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
        assertThat(output.getOut() + output.getErr())
                .contains("Unhandled exception")
                .contains("내부 오류 상세");
        assertThat(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain("내부 오류 상세");
    }

    // 테스트 전용 클래스
    record TestRequest(
            @NotBlank(message = "이름은 필수입니다.") String name,
            @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다.") int minOrderAmount
    ) {}

    @ValidRange
    record RangeRequest(int start, int end) {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = RangeValidator.class)
    @interface ValidRange {
        String message() default "시작값은 종료값보다 클 수 없습니다.";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class RangeValidator implements ConstraintValidator<ValidRange, RangeRequest> {
        @Override
        public boolean isValid(RangeRequest value, ConstraintValidatorContext context) {
            return value.start() <= value.end();
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        @GetMapping("/test/business/custom-message")
        String businessCustomMessage() {
            throw new BusinessException(ErrorCode.FORBIDDEN, "사장님만 접근할 수 있습니다.");
        }

        @PostMapping("/test/validation")
        ApiResponse<Void> validate(@Valid @RequestBody TestRequest request) {
            return ApiResponse.ok("성공");
        }

        @PostMapping("/test/validation/global")
        ApiResponse<Void> validateGlobal(@Valid @RequestBody RangeRequest request) {
            return ApiResponse.ok("성공");
        }

        @GetMapping("/test/param")
        ApiResponse<Void> requiredParam(@RequestParam String keyword) {
            return ApiResponse.ok("성공");
        }

        @GetMapping("/test/data-integrity")
        String dataIntegrity() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new IllegalStateException("duplicate key value violates unique constraint uk_user_login_id"));
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("내부 오류 상세");
        }
    }
}
