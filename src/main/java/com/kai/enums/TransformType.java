package com.kai.enums;

/**
 * 转换类型枚举（重新定义，更清晰）
 */
public enum TransformType {
    /**
     * 直接赋值
     */
    DIRECT,
    
    /**
     * 函数映射（预置函数，如upperCase、lowerCase等）
     */
    FUNCTION,
    
    /**
     * Groovy脚本动态执行
     */
    GROOVY,
    
    /**
     * 字典映射
     */
    DICTIONARY,
    
    /**
     * 固定值
     */
    FIXED,
    
    /**
     * 忽略此字段
     */
    IGNORE
}
