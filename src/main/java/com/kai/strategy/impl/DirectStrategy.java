package com.kai.strategy.impl;

import com.kai.strategy.TransformStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 直接赋值策略
 * 直接将源字段值复制到目标字段
 */
@Component
public class DirectStrategy implements TransformStrategy {
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        // 直接返回源值，不做任何转换
        return sourceValue;
    }
    
    @Override
    public String getType() {
        return "DIRECT";
    }
}

