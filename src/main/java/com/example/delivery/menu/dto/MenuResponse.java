package com.example.delivery.menu.dto;

import com.example.delivery.menu.entity.Menu;

public record MenuResponse(
        Long id,
        String name,
        int price,
        String description,
        String type
) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getDescription(),
                menu.getType().getDescription()
        );
    }
}
