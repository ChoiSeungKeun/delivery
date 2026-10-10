package com.example.delivery.menu.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MenuTypeConverter implements AttributeConverter<MenuType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MenuType type) {
        return type == null ? null : type.getPriority();
    }

    @Override
    public MenuType convertToEntityAttribute(Integer priority) {
        return priority == null ? null : MenuType.from(priority);
    }
}
