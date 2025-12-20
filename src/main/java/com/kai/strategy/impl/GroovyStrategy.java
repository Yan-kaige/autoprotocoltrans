package com.kai.strategy.impl;

import com.kai.strategy.TransformStrategy;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Groovy脚本策略
 * 通过Groovy脚本动态执行转换逻辑
 * 支持接收单个值或List<Object>作为输入
 */
@Slf4j
@Component
public class GroovyStrategy implements TransformStrategy {
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        String script = (String) ruleConfig.get("groovyScript");
        if (script == null || script.trim().isEmpty()) {
            log.warn("Groovy脚本为空，返回原值");
            return sourceValue;
        }
        
        try {
            // 创建绑定，传入变量
            Binding binding = new Binding();
            binding.setVariable("input", sourceValue);
            
            // 如果sourceValue是List，还可以提供更方便的访问方式
            if (sourceValue instanceof java.util.List) {
                binding.setVariable("inputs", sourceValue);
            }
            
            // 使用Binding创建GroovyShell并执行脚本
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(script);
            return result;
            
        } catch (Exception e) {
            log.error("Groovy脚本执行失败: {}", e.getMessage(), e);
            // 转换失败时返回原值或null
            return sourceValue;
        }
    }
    
    @Override
    public String getType() {
        return "GROOVY";
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> ruleConfig) {
        return ruleConfig != null && ruleConfig.containsKey("groovyScript");
    }
}

