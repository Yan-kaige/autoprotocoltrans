package com.kai.enums;

/**
 * 映射类型枚举
 */
public enum MappingType {
    /**
     * 1对1映射：一个源字段映射到一个目标字段
     */
    ONE_TO_ONE,
    
    /**
     * 1对多映射：一个源字段映射到多个目标字段
     */
    ONE_TO_MANY,
    
    /**
     * 多对1映射：多个源字段映射到一个目标字段
     */
    MANY_TO_ONE
}

