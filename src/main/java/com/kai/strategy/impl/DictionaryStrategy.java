package com.kai.strategy.impl;

import com.kai.strategy.TransformStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 字典映射策略
 * 根据字典配置将源值映射为目标值
 */
@Slf4j
@Component
public class DictionaryStrategy implements TransformStrategy {
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        if (sourceValue == null) {
            return null;
        }
        
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
        return ruleConfig != null && ruleConfig.containsKey("dictionary");
    }
}

