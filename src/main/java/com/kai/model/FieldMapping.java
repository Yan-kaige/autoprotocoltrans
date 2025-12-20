package com.kai.model;

import com.kai.enums.FieldMappingType;
import com.kai.enums.TransformType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 字段映射配置
 */
@Data
public class FieldMapping {
    /**
     * 映射ID
     */
    private String id;
    
    /**
     * 映射类型（1对1、1对多、多对1）
     */
    private FieldMappingType mappingType;
    
    /**
     * 源字段路径（支持嵌套路径，如 user.name 或 user.address.city）
     */
    private String sourceField;
    
    /**
     * 目标字段路径
     */
    private String targetField;
    
    /**
     * 转换类型
     */
    private TransformType transformType;
    
    /**
     * 转换参数配置
     */
    private Map<String, Object> transformParams;
    
    /**
     * 1对多或多对1时的子映射列表
     */
    private List<FieldMapping> subMappings;
    
    /**
     * 数据类型（用于数据类型转换）
     */
    private String dataType;
    
    /**
     * 固定值（用于固定值转换）
     */
    private Object fixedValue;
    
    /**
     * 字典映射配置（key: 源值, value: 目标值）
     */
    private Map<String, String> dictMapping;
    
    /**
     * 函数名称（用于函数映射）
     */
    private String functionName;
    
    /**
     * Java方法配置（用于Java方法映射）
     */
    private String javaMethod;
}

