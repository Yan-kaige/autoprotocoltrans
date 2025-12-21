package com.kai.service;

import com.jayway.jsonpath.JsonPath;
import com.kai.enums.MappingType;
import com.kai.enums.TransformType;
import com.kai.model.MappingConfig;
import com.kai.model.MappingRule;
import com.kai.strategy.TransformStrategy;
import com.kai.util.MessageConverterUtil;
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
     * 支持 JSON 和 XML 格式的输入输出
     * 
     * @param sourceData 源数据字符串（JSON或XML）
     * @param config 映射配置（包含sourceProtocol和targetProtocol）
     * @return 转换后的数据字符串（JSON或XML）
     */
    public String transform(String sourceData, MappingConfig config) throws Exception {
        // 1. 确定源数据类型（如果配置中没有，尝试自动检测）
        String sourceType = config.getSourceProtocol();
        if (sourceType == null || sourceType.isEmpty()) {
            sourceType = MessageConverterUtil.isXmlFormat(sourceData) ? "XML" : "JSON";
            log.info("自动检测源数据类型: {}", sourceType);
        }
        
        // 2. 解析源数据为Map（统一使用Map作为内部表示）
        Map<String, Object> sourceMap = parseSourceToMap(sourceData, sourceType);
        
        // 3. 创建目标Map
        Map<String, Object> targetMap = new HashMap<>();
        
        // 4. 遍历规则，执行转换
        if (config.getRules() != null) {
            for (MappingRule rule : config.getRules()) {
                applyRule(sourceMap, rule, targetMap);
            }
        }
        
        // 5. 确定目标数据类型（默认与源类型相同，如果配置中有则使用配置的）
        String targetType = config.getTargetProtocol();
        if (targetType == null || targetType.isEmpty()) {
            targetType = sourceType; // 默认与源类型相同
        }
        
        // 6. 将目标Map转换为目标格式字符串
        boolean prettyPrint = config.getPrettyPrint() != null && config.getPrettyPrint();
        return MessageConverterUtil.mapToString(targetMap, targetType, prettyPrint);
    }
    
    /**
     * 解析源数据为Map（根据类型选择解析方式）
     */
    private Map<String, Object> parseSourceToMap(String sourceData, String sourceType) throws Exception {
        try {
            // 使用 MessageConverterUtil 统一解析
            Map<String, Object> sourceMap = MessageConverterUtil.parseToMap(sourceData, sourceType);
            
            // 对于 XML，Jackson 可能会产生特殊的结构，需要适配 JsonPath
            // JsonPath 需要 JSON 格式的数据，所以需要确保 Map 结构正确
            // 如果根节点不是 Map，包装一下
            if (!(sourceMap instanceof Map)) {
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("root", sourceMap);
                return wrapper;
            }
            
            return sourceMap;
        } catch (Exception e) {
            log.error("解析{}数据失败: {}", sourceType, e.getMessage(), e);
            throw e;
        }
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
     * 支持两种模式：
     * 1. 对象拆分：源值是Map对象，通过subMappings将对象的子字段映射到多个目标路径
     * 2. 字符串拆分：源值是字符串，通过Groovy脚本拆分成Map，然后通过subMappings映射到多个目标路径
     */
    private void applyOneToManyMapping(Map<String, Object> sourceMap, MappingRule rule, Map<String, Object> targetMap) {
        // 读取源值
        Object sourceValue = readJsonPath(sourceMap, rule.getSourcePath());
        
        // 如果有子映射配置，使用子映射
        if (rule.getTransformConfig() != null && rule.getTransformConfig().containsKey("subMappings")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> subMappings = (List<Map<String, String>>) rule.getTransformConfig().get("subMappings");
            if (subMappings != null) {
                Map<String, Object> splitResult = null;
                
                // 如果源值是Map对象，直接使用
                if (sourceValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sourceObj = (Map<String, Object>) sourceValue;
                    splitResult = sourceObj;
                } 
                // 如果源值是字符串，且使用了Groovy脚本，执行脚本拆分
                else if (sourceValue instanceof String && rule.getTransformType() == TransformType.GROOVY) {
                    // 执行Groovy脚本，期望返回一个Map
                    Object transformedValue = executeTransform(sourceValue, rule);
                    if (transformedValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> groovyResult = (Map<String, Object>) transformedValue;
                        splitResult = groovyResult;
                    }
                }
                
                // 根据子映射配置，将拆分结果映射到目标路径
                if (splitResult != null) {
                    for (Map<String, String> subMapping : subMappings) {
                        String subSourcePath = subMapping.get("sourcePath");
                        String subTargetPath = subMapping.get("targetPath");
                        if (subSourcePath != null && subTargetPath != null) {
                            // 如果subSourcePath为空，表示使用索引（用于数组拆分）
                            if (subSourcePath.isEmpty()) {
                                // 如果splitResult是List，使用索引访问
                                if (splitResult instanceof java.util.List) {
                                    @SuppressWarnings("unchecked")
                                    java.util.List<Object> listResult = (java.util.List<Object>) splitResult;
                                    // 尝试从targetPath中提取索引，或者使用subMapping中的index字段
                                    String indexStr = subMapping.get("index");
                                    if (indexStr != null) {
                                        try {
                                            int index = Integer.parseInt(indexStr);
                                            if (index >= 0 && index < listResult.size()) {
                                                PathUtil.setDeepValue(targetMap, subTargetPath, listResult.get(index));
                                            }
                                        } catch (NumberFormatException e) {
                                            log.warn("无效的索引: {}", indexStr);
                                        }
                                    }
                                }
                            } else {
                                // 使用路径访问Map中的值
                                Object subValue = PathUtil.getDeepValue(splitResult, subSourcePath);
                                if (subValue != null) {
                                    PathUtil.setDeepValue(targetMap, subTargetPath, subValue);
                                }
                            }
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
     * 将Map转换为JSON字符串，然后使用JsonPath读取
     */
    private Object readJsonPath(Map<String, Object> sourceMap, String jsonPath) {
        try {
            // 将Map转换为JSON字符串，然后使用JsonPath读取
            String json = MessageConverterUtil.mapToString(sourceMap, "JSON", false);
            Object document = JsonPath.parse(json).read(jsonPath);
            return document;
        } catch (Exception e) {
            // 添加详细日志用于调试
            try {
                String json = MessageConverterUtil.mapToString(sourceMap, "JSON", true);
                log.warn("JsonPath读取失败: 路径={}, 错误={}, 源数据JSON=\n{}", jsonPath, e.getMessage(), json);
            } catch (Exception ex) {
                log.warn("JsonPath读取失败: 路径={}, 错误={}, 无法序列化源数据", jsonPath, e.getMessage());
            }
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
    
}

