package com.example.delivery.menu.controller;

import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.response.PageResponse;
import com.example.delivery.global.security.CustomUserDetails;
import com.example.delivery.menu.dto.MenuCreateRequest;
import com.example.delivery.menu.dto.MenuResponse;
import com.example.delivery.menu.entity.MenuType;
import com.example.delivery.menu.service.MenuService;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MenuControllerTest.MethodSecurityConfig.class)
class MenuControllerTest {

    private static final String MENU_URL = "/api/menus";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MenuService menuService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, UserRole role) {
        User user = User.builder().loginId("user01").password("encoded").role(role).build();
        ReflectionTestUtils.setField(user, "id", userId);
        CustomUserDetails principal = new CustomUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("OWNER가 올바른 요청으로 메뉴를 등록하면, 201과 등록된 메뉴 정보를 반환하고 로그인한 회원의 ID로 서비스에 등록을 요청한다")
    void create_success() throws Exception {
        // given
        authenticateAs(1L, UserRole.OWNER);
        String body = """
                { "name": "치킨", "price": 18000, "description": "바삭한 후라이드", "type": "MAIN" }
                """;
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, "바삭한 후라이드", MenuType.MAIN);
        given(menuService.create(1L, request))
                .willReturn(new MenuResponse(1L, "치킨", 18000, "바삭한 후라이드", "메인 메뉴"));

        // when
        ResultActions result = mockMvc.perform(post(MENU_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("메뉴가 등록되었습니다."))
                .andExpect(jsonPath("$.data.name").value("치킨"))
                .andExpect(jsonPath("$.data.price").value(18000))
                .andExpect(jsonPath("$.data.description").value("바삭한 후라이드"))
                .andExpect(jsonPath("$.data.type").value("메인 메뉴"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        verify(menuService).create(1L, request);
    }

    @Test
    @DisplayName("CUSTOMER가 메뉴를 등록하려고 하면, 403과 FORBIDDEN 코드와 메시지를 반환하고 서비스는 호출되지 않는다")
    void create_customer_forbidden() throws Exception {
        // given
        authenticateAs(2L, UserRole.CUSTOMER);
        String body = """
                { "name": "치킨", "price": 18000, "description": "바삭한 후라이드", "type": "MAIN" }
                """;

        // when
        ResultActions result = mockMvc.perform(post(MENU_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FORBIDDEN.getMessage()));

        verify(menuService, never()).create(any(), any());
    }

    @Test
    @DisplayName("OWNER가 이름이 비어 있고 가격이 0원인 요청을 보내면, 400과 INVALID_INPUT 코드와 name, price 필드 오류를 반환하고 서비스는 호출되지 않는다")
    void create_invalidRequest() throws Exception {
        // given
        authenticateAs(1L, UserRole.OWNER);
        String body = """
                { "name": "", "price": 0, "description": "바삭한 후라이드", "type": "MAIN" }
                """;

        // when
        ResultActions result = mockMvc.perform(post(MENU_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].reason").value("메뉴 이름은 필수입니다."))
                .andExpect(jsonPath("$.errors[?(@.field == 'price')].reason").value("가격은 1원 이상이어야 합니다."));

        verify(menuService, never()).create(any(), any());
    }

    @Test
    @DisplayName("존재하는 메뉴 ID(1)로 단건 조회하면, 인증 없이도 200과 해당 메뉴의 id, 이름, 가격, 타입 설명을 반환한다")
    void getMenu_success() throws Exception {
        // given
        given(menuService.getMenu(1L))
                .willReturn(new MenuResponse(1L, "치킨", 18000, "바삭한 후라이드", "메인 메뉴"));

        // when
        ResultActions result = mockMvc.perform(get(MENU_URL + "/1"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("치킨"))
                .andExpect(jsonPath("$.data.price").value(18000))
                .andExpect(jsonPath("$.data.type").value("메인 메뉴"));
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 메뉴 ID(999)로 단건 조회하면, 404와 MENU_NOT_FOUND 코드와 메시지를 반환한다")
    void getMenu_notFound() throws Exception {
        // given
        given(menuService.getMenu(999L)).willThrow(new BusinessException(ErrorCode.MENU_NOT_FOUND));

        // when
        ResultActions result = mockMvc.perform(get(MENU_URL + "/999"));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.MENU_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.MENU_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("정렬 조건 없이 목록을 조회하면, 200과 페이징 정보를 반환하고 서비스에는 0페이지, 10개, type 오름차순이 전달된다")
    void getMenus_default() throws Exception {
        // given
        MenuResponse menu = new MenuResponse(1L, "치킨", 18000, "바삭한 후라이드", "대표 메뉴");
        given(menuService.getMenus(any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(menu), 0, 10, 1L, 1, false));

        // when
        ResultActions result = mockMvc.perform(get(MENU_URL));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(menuService).getMenus(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("type").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("sort=price,desc와 page=1로 목록을 조회하면, 서비스에는 1페이지와 price 내림차순이 전달된다")
    void getMenus_withSortParam() throws Exception {
        // given
        given(menuService.getMenus(any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(), 1, 10, 0L, 0, false));

        // when
        ResultActions result = mockMvc.perform(get(MENU_URL).param("page", "1").param("sort", "price,desc"));

        // then
        result.andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(menuService).getMenus(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

}