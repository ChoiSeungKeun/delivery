package com.example.delivery.menu.dto;

import com.example.delivery.menu.entity.Menu;
import com.example.delivery.menu.entity.MenuType;
import com.example.delivery.user.entity.User;
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

class MenuCreateRequestTest {

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

    @Test
    @DisplayName("이름, 1원 이상의 가격, 설명, 메뉴 타입이 모두 올바르면, 검증을 통과한다")
    void validate_success() {
        // given
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, "바삭한 후라이드", MenuType.MAIN);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이름 50자, 가격 1원, 설명 200자(경계값)이고 메뉴 타입이 null이면, 검증을 통과한다")
    void validate_boundary_success() {
        // given
        MenuCreateRequest request = new MenuCreateRequest("a".repeat(50), 1, "b".repeat(200), null);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("설명이 null이면, 검증을 통과한다")
    void validate_descriptionNull_success() {
        // given
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, null, MenuType.MAIN);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("이름이 빈 문자열이거나 공백이면, name 필드 검증에 실패한다")
    void validate_nameBlank_fail(String name) {
        // given
        MenuCreateRequest request = new MenuCreateRequest(name, 18000, "바삭한 후라이드", MenuType.MAIN);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("name");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("메뉴 이름은 필수입니다.");
    }

    @Test
    @DisplayName("이름이 51자이면, name 필드 검증에 실패한다")
    void validate_nameTooLong_fail() {
        // given
        MenuCreateRequest request = new MenuCreateRequest("a".repeat(51), 18000, "바삭한 후라이드", MenuType.MAIN);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("name");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("메뉴 이름은 50자 이하여야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("가격이 0원 이하이면, price 필드 검증에 실패한다")
    void validate_priceTooLow_fail(int price) {
        // given
        MenuCreateRequest request = new MenuCreateRequest("치킨", price, "바삭한 후라이드", MenuType.MAIN);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("price");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("가격은 1원 이상이어야 합니다.");
    }

    @Test
    @DisplayName("설명이 201자이면, description 필드 검증에 실패한다")
    void validate_descriptionTooLong_fail() {
        // given
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, "b".repeat(201), MenuType.MAIN);

        // when
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("description");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("메뉴 설명은 200자 이하여야 합니다.");
    }

    @Test
    @DisplayName("회원과 함께 엔티티로 변환하면, 회원과 이름, 가격, 설명, 메뉴 타입이 그대로 담긴 Menu가 만들어진다")
    void toEntity_mapsFields() {
        // given
        User user = User.builder().loginId("owner01").password("encoded").role(UserRole.OWNER).build();
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, "바삭한 후라이드", MenuType.REPRESENTATIVE);

        // when
        Menu menu = request.toEntity(user);

        // then
        assertThat(menu.getUser()).isSameAs(user);
        assertThat(menu.getName()).isEqualTo("치킨");
        assertThat(menu.getPrice()).isEqualTo(18000);
        assertThat(menu.getDescription()).isEqualTo("바삭한 후라이드");
        assertThat(menu.getType()).isEqualTo(MenuType.REPRESENTATIVE);
    }

    @Test
    @DisplayName("메뉴 타입이 null인 요청을 엔티티로 변환하면, 메뉴 타입은 기본값인 MAIN으로 설정된다")
    void toEntity_nullType_defaultsToMain() {
        // given
        User user = User.builder().loginId("owner01").password("encoded").role(UserRole.OWNER).build();
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, null, null);

        // when
        Menu menu = request.toEntity(user);

        // then
        assertThat(menu.getType()).isEqualTo(MenuType.MAIN);
    }

}