package com.example.delivery.menu.service;

import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.response.PageResponse;
import com.example.delivery.menu.dto.MenuCreateRequest;
import com.example.delivery.menu.dto.MenuResponse;
import com.example.delivery.menu.entity.Menu;
import com.example.delivery.menu.entity.MenuType;
import com.example.delivery.menu.repository.MenuRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    MenuRepository menuRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    MenuService menuService;

    @Test
    @DisplayName("회원 ID와 메뉴 정보로 메뉴를 등록하면, 해당 회원의 메뉴로 저장하고 등록된 메뉴 정보와 메뉴 타입 설명을 반환한다")
    void create_success() {
        // given
        User owner = User.builder().loginId("owner01").password("encoded").role(UserRole.OWNER).build();
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, "바삭한 후라이드", MenuType.REPRESENTATIVE);
        given(userRepository.getReferenceById(1L)).willReturn(owner);
        given(menuRepository.save(any(Menu.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        MenuResponse response = menuService.create(1L, request);

        // then
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());

        Menu saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getName()).isEqualTo("치킨");
        assertThat(saved.getPrice()).isEqualTo(18000);
        assertThat(saved.getDescription()).isEqualTo("바삭한 후라이드");
        assertThat(saved.getType()).isEqualTo(MenuType.REPRESENTATIVE);

        assertThat(response.name()).isEqualTo("치킨");
        assertThat(response.price()).isEqualTo(18000);
        assertThat(response.description()).isEqualTo("바삭한 후라이드");
        assertThat(response.type()).isEqualTo("대표 메뉴");
    }

    @Test
    @DisplayName("메뉴 타입 없이 메뉴를 등록하면, 기본값인 MAIN으로 저장하고 응답에는 '메인 메뉴'를 반환한다")
    void create_nullType_defaultsToMain() {
        // given
        User owner = User.builder().loginId("owner01").password("encoded").role(UserRole.OWNER).build();
        MenuCreateRequest request = new MenuCreateRequest("치킨", 18000, null, null);
        given(userRepository.getReferenceById(1L)).willReturn(owner);
        given(menuRepository.save(any(Menu.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        MenuResponse response = menuService.create(1L, request);

        // then
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MenuType.MAIN);
        assertThat(response.type()).isEqualTo("메인 메뉴");
    }

    @Test
    @DisplayName("존재하는 메뉴 ID(1)로 단건 조회하면, 해당 메뉴의 id, 이름, 가격, 타입 설명을 반환한다")
    void getMenu_success() {
        // given
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu(1L, MenuType.REPRESENTATIVE)));

        // when
        MenuResponse response = menuService.getMenu(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("치킨");
        assertThat(response.price()).isEqualTo(18000);
        assertThat(response.type()).isEqualTo("대표 메뉴");
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 메뉴 ID(999)로 단건 조회하면, MENU_NOT_FOUND 예외가 발생한다")
    void getMenu_notFound() {
        // given
        given(menuRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuService.getMenu(999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND));
    }

    @Test
    @DisplayName("type 오름차순으로 목록을 조회하면, 조회 시 id 내림차순이 보조 정렬로 추가되고 메뉴 목록과 페이징 정보를 반환한다")
    void getMenus_appendsIdSort() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "type"));
        given(menuRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(menu(1L, MenuType.REPRESENTATIVE)), pageable, 1));

        // when
        PageResponse<MenuResponse> response = menuService.getMenus(pageable);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(menuRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.asc("type"), Sort.Order.desc("id")));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("id 오름차순으로 목록을 조회하면, id 정렬이 이미 있으므로 보조 정렬을 추가하지 않고 그대로 조회한다")
    void getMenus_idSort_notDuplicated() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        given(menuRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        menuService.getMenus(pageable);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(menuRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    @DisplayName("허용되지 않은 name 기준으로 목록을 조회하면, INVALID_INPUT 예외가 발생하고 DB는 조회하지 않는다")
    void getMenus_invalidSort() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));

        // when & then
        assertThatThrownBy(() -> menuService.getMenus(pageable))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        verify(menuRepository, never()).findAll(any(Pageable.class));
    }

    private Menu menu(Long id, MenuType type) {
        User owner = User.builder().loginId("owner01").password("encoded").role(UserRole.OWNER).build();
        Menu menu = Menu.builder().user(owner).name("치킨").price(18000).description("바삭한 후라이드").type(type).build();
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

}