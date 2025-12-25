package com.kai.strategy.impl;

import com.kai.service.DictionaryService;
import com.kai.strategy.TransformStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 字典映射策略
 * 根据字典配置将源值映射为目标值
 * 支持通过字典ID引用字典，支持双向转换（k->v 或 v->k）
 */
@Slf4j
@Component
public class DictionaryStrategy implements TransformStrategy {
    
    @Autowired
    private DictionaryService dictionaryService;
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        if (sourceValue == null) {
            return null;
        }
        
        // 优先使用字典ID方式（新方式）
        if (ruleConfig != null && ruleConfig.containsKey("dictionaryId")) {
            Long dictionaryId = ((Number) ruleConfig.get("dictionaryId")).longValue();
            Boolean reverse = ruleConfig.containsKey("dictionaryDirection") 
                    ? (Boolean) ruleConfig.get("dictionaryDirection") 
                    : false;
            
            try {
                Map<String, String> dictionary = dictionaryService.getDictionaryMap(dictionaryId, reverse);
                if (dictionary == null || dictionary.isEmpty()) {
                    log.warn("字典ID {} 不存在或为空，返回原值", dictionaryId);
                    return sourceValue;
                }
                
                String sourceKey = sourceValue.toString();
                String mappedValue = dictionary.get(sourceKey);
                
                // 如果字典中没有找到映射，返回原值
                if (mappedValue == null) {
                    log.warn("字典中未找到键: {}, 返回原值", sourceKey);
                    return sourceValue;
                }
                
                return mappedValue;
            } catch (Exception e) {
                log.error("使用字典ID {} 转换失败: {}", dictionaryId, e.getMessage(), e);
                return sourceValue;
            }
        }
        
        // 兼容旧方式：直接使用dictionary配置（向后兼容）
        @SuppressWarnings("unchecked")
        Map<String, String> dictionary = (Map<String, String>) ruleConfig.get("dictionary");
        if (dictionary == null || dictionary.isEmpty()) {
            log.warn("字典配置为空，返回原值");
            return sourceValue;
        }
        
        String sourceKey = sourceValue.toString();
        String mappedValue = dictionary.get(sourceKey);
        
        // 如果字典中没有找到映射，返回原值
        if (mappedValue == null) {
            log.warn("字典中未找到键: {}, 返回原值", sourceKey);
            return sourceValue;
        }
        
        return mappedValue;
    }
    
    @Override
    public String getType() {
        return "DICTIONARY";
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> ruleConfig) {
        // 支持两种方式：字典ID或直接字典配置
        return ruleConfig != null && 
                (ruleConfig.containsKey("dictionaryId") || ruleConfig.containsKey("dictionary"));
    }
}

