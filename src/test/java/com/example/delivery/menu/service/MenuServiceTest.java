package com.example.delivery.menu.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

}