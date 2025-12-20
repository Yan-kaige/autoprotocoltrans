package com.kai.strategy;

import java.util.Map;

/**
 * 转换策略接口
 * 使用策略模式，支持多种转换类型的扩展
 */
public interface TransformStrategy {
    
    /**
     * 执行转换
     * 
     * @param sourceValue 源字段值
     *                     对于1对1映射：单个值（Object）
     *                     对于多对1映射：List<Object>，包含多个源字段的值
     * @param ruleConfig 规则配置Map，包含转换所需的参数
     *                   例如：groovyScript、dictionary、fixedValue等
     * @return 转换后的值
     */
    Object transform(Object sourceValue, Map<String, Object> ruleConfig);
    
    /**
     * 获取策略类型名称
     * 用于策略注册和查找
     * 
     * @return 策略类型，如 "DIRECT", "GROOVY", "DICTIONARY" 等
     */
    String getType();
    
    /**
     * 验证规则配置是否有效
     * 
     * @param ruleConfig 规则配置
     * @return 是否有效
     */
    default boolean validateConfig(Map<String, Object> ruleConfig) {
        return true;
    }
}

