package com.kai.service;

import com.kai.enums.TransformType;
import com.kai.model.FieldMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 字段转换服务
 * 根据转换类型执行相应的转换逻辑
 */
@Service
public class FieldTransformService {
    
    @Autowired
    private TransformFunctionService functionService;
    
    /**
     * 转换字段值
     */
    public Object transformValue(Object sourceValue, FieldMapping mapping) {
        if (mapping.getTransformType() == null) {
            return sourceValue;
        }
        
        TransformType type = mapping.getTransformType();
        
        switch (type) {
            case DIRECT:
                return transformDirect(sourceValue);
            case FUNCTION:
                return transformFunction(sourceValue, mapping);
            case GROOVY:
                // Groovy转换已在新策略模式中实现，这里保留兼容
                return transformGroovy(sourceValue, mapping);
            case DICTIONARY:
                return transformDictionary(sourceValue, mapping);
            case FIXED:
                return transformFixed(mapping);
            case IGNORE:
                return null; // 忽略字段，返回null
            default:
                return sourceValue;
        }
    }
    
    /**
     * 直接转换（直接复制）
     */
    private Object transformDirect(Object value) {
        return value;
    }
    
    /**
     * 函数映射转换
     */
    private Object transformFunction(Object value, FieldMapping mapping) {
        String functionName = mapping.getFunctionName();
        if (functionName == null || functionName.isEmpty()) {
            return value;
        }
        
        try {
            return functionService.executeFunction(functionName, value);
        } catch (Exception e) {
            // 转换失败返回原值
            return value;
        }
    }
    
    /**
     * Groovy脚本转换（已在新策略模式中实现，这里保留兼容）
     */
    private Object transformGroovy(Object value, FieldMapping mapping) {
        // 注意：新的实现已使用GroovyStrategy，这里保留为空实现以兼容旧代码
        // 实际使用时应该使用新的TransformationEngine
        return value;
    }
    
    /**
     * 字典映射转换
     */
    private Object transformDictionary(Object value, FieldMapping mapping) {
        if (value == null || mapping.getDictMapping() == null) {
            return value;
        }
        
        String sourceValue = value.toString();
        String mappedValue = mapping.getDictMapping().get(sourceValue);
        
        // 如果字典中没有找到映射，返回原值或null
        return mappedValue != null ? mappedValue : value;
    }
    
    /**
     * 固定值转换
     */
    private Object transformFixed(FieldMapping mapping) {
        return mapping.getFixedValue();
    }
}

