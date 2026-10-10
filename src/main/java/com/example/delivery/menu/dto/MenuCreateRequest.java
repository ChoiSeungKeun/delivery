package com.example.delivery.menu.dto;

import com.example.delivery.menu.entity.Menu;
import com.example.delivery.menu.entity.MenuType;
import com.example.delivery.user.entity.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank(message = "메뉴 이름은 필수입니다.")
        @Size(max = 50, message = "메뉴 이름은 50자 이하여야 합니다.")
        String name,

        @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
        int price,

        @Size(max = 200, message = "메뉴 설명은 200자 이하여야 합니다.")
        String description,

        MenuType type
) {
    public Menu toEntity(User user) {
        return Menu.builder()
                .user(user)
                .name(name)
                .price(price)
                .description(description)
                .type(type)
                .build();
    }
}
