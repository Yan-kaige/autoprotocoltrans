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
            case EQUAL:
                return transformEqual(sourceValue);
            case FUNCTION:
                return transformFunction(sourceValue, mapping);
            case JAVA_METHOD:
                return transformJavaMethod(sourceValue, mapping);
            case DATA_TYPE:
                return transformDataType(sourceValue, mapping);
            case IGNORE:
                return null; // 忽略字段，返回null
            case DICT:
                return transformDict(sourceValue, mapping);
            case FIXED:
                return transformFixed(mapping);
            default:
                return sourceValue;
        }
    }
    
    /**
     * 等于转换（直接复制）
     */
    private Object transformEqual(Object value) {
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
     * Java方法映射转换（这里简化处理，实际可以支持动态调用）
     */
    private Object transformJavaMethod(Object value, FieldMapping mapping) {
        // TODO: 实现Java方法动态调用
        // 可以通过Groovy脚本或反射实现
        return value;
    }
    
    /**
     * 数据类型转换
     */
    private Object transformDataType(Object value, FieldMapping mapping) {
        if (value == null) {
            return null;
        }
        
        String dataType = mapping.getDataType();
        if (dataType == null) {
            return value;
        }
        
        try {
            switch (dataType.toLowerCase()) {
                case "string":
                    return value.toString();
                case "int":
                case "integer":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                case "long":
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(value.toString());
                case "double":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                case "boolean":
                    if (value instanceof Boolean) {
                        return value;
                    }
                    return Boolean.parseBoolean(value.toString());
                default:
                    return value;
            }
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * 字典映射转换
     */
    private Object transformDict(Object value, FieldMapping mapping) {
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

