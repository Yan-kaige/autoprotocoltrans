package com.kai.service;

import com.kai.enums.FieldMappingType;
import com.kai.enums.ProtocolType;
import com.kai.model.FieldMapping;
import com.kai.model.TransformRule;
import com.kai.util.FieldPathUtil;
import com.kai.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换引擎核心类
 */
@Service
public class TransformEngine {
    
    @Autowired
    private FieldTransformService fieldTransformService;
    
    /**
     * 执行转换
     */
    public String transform(TransformRule rule, String sourceData, boolean prettyPrint) throws Exception {
        // 1. 解析源数据
        Map<String, Object> sourceMap = parseSourceData(sourceData, rule.getSourceType());
        
        // 2. 创建目标Map
        Map<String, Object> targetMap = new HashMap<>();
        
        // 3. 执行字段映射
        if (rule.getFieldMappings() != null) {
            for (FieldMapping mapping : rule.getFieldMappings()) {
                applyMapping(sourceMap, targetMap, mapping);
            }
        }
        
        // 4. 转换为目标格式
        return formatTargetData(targetMap, rule.getTargetType(), prettyPrint);
    }
    
    /**
     * 解析源数据
     */
    private Map<String, Object> parseSourceData(String sourceData, ProtocolType sourceType) throws Exception {
        if (sourceType == ProtocolType.JSON) {
            return JsonUtil.jsonToMap(sourceData);
        } else if (sourceType == ProtocolType.XML) {
            return JsonUtil.xmlToMap(sourceData);
        }
        throw new IllegalArgumentException("不支持的源协议类型: " + sourceType);
    }
    
    /**
     * 格式化目标数据
     */
    private String formatTargetData(Map<String, Object> targetMap, ProtocolType targetType, boolean prettyPrint) throws Exception {
        if (targetType == ProtocolType.JSON) {
            return JsonUtil.mapToJson(targetMap, prettyPrint);
        } else if (targetType == ProtocolType.XML) {
            return JsonUtil.mapToXml(targetMap, prettyPrint);
        }
        throw new IllegalArgumentException("不支持的目标协议类型: " + targetType);
    }
    
    /**
     * 应用字段映射
     */
    private void applyMapping(Map<String, Object> sourceMap, Map<String, Object> targetMap, FieldMapping mapping) {
        FieldMappingType mappingType = mapping.getMappingType();
        if (mappingType == null) {
            mappingType = FieldMappingType.ONE_TO_ONE;
        }
        
        switch (mappingType) {
            case ONE_TO_ONE:
                applyOneToOneMapping(sourceMap, targetMap, mapping);
                break;
            case ONE_TO_MANY:
                applyOneToManyMapping(sourceMap, targetMap, mapping);
                break;
            case MANY_TO_ONE:
                applyManyToOneMapping(sourceMap, targetMap, mapping);
                break;
        }
    }
    
    /**
     * 1对1映射
     */
    private void applyOneToOneMapping(Map<String, Object> sourceMap, Map<String, Object> targetMap, FieldMapping mapping) {
        // 如果转换类型是忽略，直接返回
        if (mapping.getTransformType() != null && mapping.getTransformType().name().equals("IGNORE")) {
            return;
        }
        
        Object sourceValue = FieldPathUtil.getValueByPath(sourceMap, mapping.getSourceField());
        Object transformedValue = fieldTransformService.transformValue(sourceValue, mapping);
        
        if (transformedValue != null || mapping.getTransformType() == null || !mapping.getTransformType().name().equals("IGNORE")) {
            FieldPathUtil.setValueByPath(targetMap, mapping.getTargetField(), transformedValue);
        }
    }
    
    /**
     * 1对多映射
     */
    private void applyOneToManyMapping(Map<String, Object> sourceMap, Map<String, Object> targetMap, FieldMapping mapping) {
        Object sourceValue = FieldPathUtil.getValueByPath(sourceMap, mapping.getSourceField());
        
        if (mapping.getSubMappings() != null && !mapping.getSubMappings().isEmpty()) {
            // 使用子映射将源字段值映射到多个目标字段
            for (FieldMapping subMapping : mapping.getSubMappings()) {
                // 子映射的源字段使用父映射的源字段值
                Object transformedValue = fieldTransformService.transformValue(sourceValue, subMapping);
                FieldPathUtil.setValueByPath(targetMap, subMapping.getTargetField(), transformedValue);
            }
        }
    }
    
    /**
     * 多对1映射
     */
    private void applyManyToOneMapping(Map<String, Object> sourceMap, Map<String, Object> targetMap, FieldMapping mapping) {
        if (mapping.getSubMappings() != null && !mapping.getSubMappings().isEmpty()) {
            // 创建一个Map来存储多个源字段的值
            Map<String, Object> combinedValue = new HashMap<>();
            
            for (FieldMapping subMapping : mapping.getSubMappings()) {
                Object sourceValue = FieldPathUtil.getValueByPath(sourceMap, subMapping.getSourceField());
                Object transformedValue = fieldTransformService.transformValue(sourceValue, subMapping);
                // 使用源字段名作为key（或者可以使用subMapping的配置）
                String key = subMapping.getSourceField().contains(".") 
                    ? subMapping.getSourceField().substring(subMapping.getSourceField().lastIndexOf(".") + 1)
                    : subMapping.getSourceField();
                combinedValue.put(key, transformedValue);
            }
            
            // 将合并后的值设置到目标字段
            FieldPathUtil.setValueByPath(targetMap, mapping.getTargetField(), combinedValue);
        }
    }
}

