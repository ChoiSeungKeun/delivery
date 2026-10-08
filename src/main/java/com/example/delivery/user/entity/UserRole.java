package com.example.delivery.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    CUSTOMER("손님"),
    OWNER("사장님");

    private final String description;
}
