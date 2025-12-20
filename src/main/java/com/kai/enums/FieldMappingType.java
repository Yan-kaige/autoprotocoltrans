package com.kai.enums;

/**
 * 字段映射类型枚举
 */
public enum FieldMappingType {
    ONE_TO_ONE("1对1"),
    ONE_TO_MANY("1对多"),
    MANY_TO_ONE("多对1");

    private final String description;

    FieldMappingType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

