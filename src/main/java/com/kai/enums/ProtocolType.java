package com.kai.enums;

/**
 * 协议类型枚举
 */
public enum ProtocolType {
    JSON("JSON"),
    XML("XML");

    private final String name;

    ProtocolType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

