package com.example.delivery.menu.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MenuType {

    REPRESENTATIVE(1, "대표 메뉴"),
    MAIN(2, "메인 메뉴"),
    SIDE(3, "사이드 메뉴");

    private final int priority;
    private final String description;

    public static MenuType from(int priority) {
        return Arrays.stream(values())
                .filter(type -> type.priority == priority)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 우선순위입니다: " + priority));
    }
}
