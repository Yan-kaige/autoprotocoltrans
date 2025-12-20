package com.kai.enums;

/**
 * 转换类型枚举
 */
public enum TransformType {
    EQUAL("等于", "直接复制字段值"),
    FUNCTION("函数映射", "使用预置函数转换"),
    JAVA_METHOD("Java方法映射", "调用配置的Java方法"),
    DATA_TYPE("数据类型转换", "转换数据类型"),
    IGNORE("忽略", "忽略此字段"),
    DICT("字典映射", "通过字典映射值"),
    FIXED("固定值", "转为固定字段值");

    private final String name;
    private final String description;

    TransformType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

