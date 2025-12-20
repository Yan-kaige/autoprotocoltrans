package com.kai.strategy.impl;

import com.kai.strategy.TransformStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 固定值策略
 * 无论源值是什么，都返回配置的固定值
 */
@Component
public class FixedStrategy implements TransformStrategy {
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        // 忽略sourceValue，直接返回固定值
        return ruleConfig.get("fixedValue");
    }
    
    @Override
    public String getType() {
        return "FIXED";
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> ruleConfig) {
        return ruleConfig != null && ruleConfig.containsKey("fixedValue");
    }
}

