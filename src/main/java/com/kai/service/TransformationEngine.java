package com.kai.service;

import com.jayway.jsonpath.JsonPath;
import com.kai.enums.MappingType;
import com.kai.enums.TransformType;
import com.kai.model.MappingConfig;
import com.kai.model.MappingRule;
import com.kai.strategy.TransformStrategy;
import com.kai.util.PathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 转换引擎核心类
 * 负责执行报文转换的核心逻辑
 */
@Slf4j
@Service
public class TransformationEngine {
    
    private final Map<String, TransformStrategy> strategyMap = new HashMap<>();
    
    @Autowired
    public TransformationEngine(List<TransformStrategy> strategies) {
        // 注册所有策略
        for (TransformStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
            log.info("注册转换策略: {}", strategy.getType());
        }
    }
    
    /**
     * 执行转换
     * 
     * @param sourceJson 源JSON字符串
     * @param config 映射配置
     * @return 转换后的JSON字符串
     */
    public String transform(String sourceJson, MappingConfig config) throws Exception {
        // 1. 解析源JSON为Map
        Map<String, Object> sourceMap = parseJsonToMap(sourceJson);
        
        // 2. 创建目标Map
        Map<String, Object> targetMap = new HashMap<>();
        
        // 3. 遍历规则，执行转换
        if (config.getRules() != null) {
            for (MappingRule rule : config.getRules()) {
                applyRule(sourceMap, rule, targetMap);
            }
        }
        
        // 4. 将目标Map转换为JSON字符串
        return mapToJson(targetMap, config.getPrettyPrint() != null && config.getPrettyPrint());
    }
    
    /**
     * 解析JSON字符串为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) throws Exception {
        Object document = JsonPath.parse(json).json();
        if (document instanceof Map) {
            return (Map<String, Object>) document;
        }
        // 如果根节点不是Map，包装一下
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("root", document);
        return wrapper;
    }
    
    /**
     * 应用单个映射规则
     */
    private void applyRule(Map<String, Object> sourceMap, MappingRule rule, Map<String, Object> targetMap) {
        if (rule.getTransformType() == TransformType.IGNORE) {
            return;
        }
        
        MappingType mappingType = rule.getMappingType();
        if (mappingType == null) {
            mappingType = MappingType.ONE_TO_ONE;
        }
        
        switch (mappingType) {
            case ONE_TO_ONE:
                applyOneToOneMapping(sourceMap, rule, targetMap);
                break;
            case ONE_TO_MANY:
                applyOneToManyMapping(sourceMap, rule, targetMap);
                break;
            case MANY_TO_ONE:
                applyManyToOneMapping(sourceMap, rule, targetMap);
                break;
        }
    }
    
    /**
     * 1对1映射
     */
    private void applyOneToOneMapping(Map<String, Object> sourceMap, MappingRule rule, Map<String, Object> targetMap) {
        try {
            Object sourceValue = null;
            
            // 如果是固定值，sourcePath可能为null
            if (rule.getSourcePath() != null && !rule.getSourcePath().isEmpty()) {
                sourceValue = readJsonPath(sourceMap, rule.getSourcePath());
            }
            
            // 执行转换
            Object transformedValue = executeTransform(sourceValue, rule);
            
            // 设置到目标路径
            if (transformedValue != null || rule.getTransformType() != TransformType.IGNORE) {
                PathUtil.setDeepValue(targetMap, rule.getTargetPath(), transformedValue);
            }
        } catch (Exception e) {
            log.error("1对1映射执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 1对多映射
     */
    private void applyOneToManyMapping(Map<String, Object> sourceMap, MappingRule rule, Map<String, Object> targetMap) {
        // 读取源值
        Object sourceValue = readJsonPath(sourceMap, rule.getSourcePath());
        
        // 如果有子映射配置，使用子映射
        if (rule.getTransformConfig() != null && rule.getTransformConfig().containsKey("subMappings")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> subMappings = (List<Map<String, String>>) rule.getTransformConfig().get("subMappings");
            if (subMappings != null && sourceValue instanceof Map) {
                Map<String, Object> sourceObj = (Map<String, Object>) sourceValue;
                for (Map<String, String> subMapping : subMappings) {
                    String subSourcePath = subMapping.get("sourcePath");
                    String subTargetPath = subMapping.get("targetPath");
                    if (subSourcePath != null && subTargetPath != null) {
                        Object subValue = PathUtil.getDeepValue(sourceObj, subSourcePath);
                        if (subValue != null) {
                            PathUtil.setDeepValue(targetMap, subTargetPath, subValue);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 多对1映射
     */
    private void applyManyToOneMapping(Map<String, Object> sourceMap, MappingRule rule, Map<String, Object> targetMap) {
        try {
            List<Object> sourceValues = new ArrayList<>();
            
            // 读取主源路径
            if (rule.getSourcePath() != null && !rule.getSourcePath().isEmpty()) {
                Object mainValue = readJsonPath(sourceMap, rule.getSourcePath());
                if (mainValue != null) {
                    sourceValues.add(mainValue);
                }
            }
            
            // 读取额外源路径
            if (rule.getAdditionalSources() != null) {
                for (String additionalPath : rule.getAdditionalSources()) {
                    Object value = readJsonPath(sourceMap, additionalPath);
                    if (value != null) {
                        sourceValues.add(value);
                    }
                }
            }
            
            // 执行转换（将List作为输入传给策略）
            Object transformedValue = executeTransform(sourceValues, rule);
            
            // 设置到目标路径
            if (transformedValue != null) {
                PathUtil.setDeepValue(targetMap, rule.getTargetPath(), transformedValue);
            }
        } catch (Exception e) {
            log.error("多对1映射执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 使用JsonPath读取值
     */
    private Object readJsonPath(Map<String, Object> sourceMap, String jsonPath) {
        try {
            // 将Map转换为JSON字符串，然后使用JsonPath读取
            String json = mapToJson(sourceMap, false);
            Object document = JsonPath.parse(json).read(jsonPath);
            return document;
        } catch (Exception e) {
            log.warn("JsonPath读取失败: {}, 错误: {}", jsonPath, e.getMessage());
            return null;
        }
    }
    
    /**
     * 执行转换策略
     */
    private Object executeTransform(Object sourceValue, MappingRule rule) {
        TransformType transformType = rule.getTransformType();
        if (transformType == null) {
            return sourceValue;
        }
        
        TransformStrategy strategy = strategyMap.get(transformType.name());
        if (strategy == null) {
            log.warn("未找到转换策略: {}", transformType);
            return sourceValue;
        }
        
        Map<String, Object> config = rule.getTransformConfig();
        if (config == null) {
            config = new HashMap<>();
        }
        
        return strategy.transform(sourceValue, config);
    }
    
    /**
     * Map转JSON字符串
     */
    private String mapToJson(Map<String, Object> map, boolean prettyPrint) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        if (prettyPrint) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        }
        return mapper.writeValueAsString(map);
    }
}

