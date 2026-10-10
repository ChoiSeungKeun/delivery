package com.example.delivery.user.dto;

import com.example.delivery.user.entity.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


class UserJoinRequestTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // 성공
    @Test
    @DisplayName("로그인 ID 4~20자, 비밀번호 8자 이상, 유효한 역할로 요청하면, 검증을 통과한다")
    void validate_success() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "password123", UserRole.CUSTOMER);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "abcdefghijklmnopqrst"})
    @DisplayName("로그인 ID가 경계값(4자, 20자)이면, 검증을 통과한다")
    void validate_loginIdBoundary_success(String loginId) {
        // given
        UserJoinRequest request = new UserJoinRequest(loginId, "password123", UserRole.CUSTOMER);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 8자(경계값)이면, 검증을 통과한다")
    void validate_passwordBoundary_success() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "pass1234", UserRole.CUSTOMER);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    // 실패
    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefghijklmnopqrstu"})
    @DisplayName("로그인 ID가 4자 미만이거나 20자 초과이면, loginId 필드 검증에 실패한다")
    void validate_loginIdLength_fail(String loginId) {
        // given
        UserJoinRequest request = new UserJoinRequest(loginId, "password123", UserRole.CUSTOMER);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("loginId");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("로그인 ID는 4~20자여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면, password 필드 검증에 실패한다")
    void validate_passwordTooShort_fail() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "pass123", UserRole.CUSTOMER);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("password");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 8자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("로그인 ID가 공백이면, loginId 필드 검증에 실패한다")
    void validate_loginIdBlank_fail() {
        // given
        UserJoinRequest request = new UserJoinRequest("   ", "password123", UserRole.CUSTOMER);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("loginId");
    }

    @Test
    @DisplayName("역할이 null이면, role 필드 검증에 실패한다")
    void validate_roleNull_fail() {
        // given
        UserJoinRequest request = new UserJoinRequest("user01", "password123", null);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("role");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("회원 역할은 필수입니다.");
    }

    @Test
    @DisplayName("로그인 ID, 비밀번호, 역할이 null이면, 세 필드 모두 검증에 실패한다")
    void validate_null_fail() {
        // given
        UserJoinRequest request = new UserJoinRequest(null, null, null);

        // when
        Set<ConstraintViolation<UserJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("loginId", "password", "role");
    }
}