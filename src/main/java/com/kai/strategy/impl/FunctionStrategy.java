package com.kai.strategy.impl;

import com.kai.strategy.TransformStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 函数映射策略
 * 使用预置的函数进行转换
 */
@Slf4j
@Component
public class FunctionStrategy implements TransformStrategy {
    
    private final Map<String, Function<Object, Object>> functions = new HashMap<>();
    
    public FunctionStrategy() {
        initDefaultFunctions();
    }
    
    /**
     * 初始化默认函数
     */
    private void initDefaultFunctions() {
        // 字符串转大写
        functions.put("upperCase", obj -> {
            if (obj == null) return null;
            return obj.toString().toUpperCase();
        });
        
        // 字符串转小写
        functions.put("lowerCase", obj -> {
            if (obj == null) return null;
            return obj.toString().toLowerCase();
        });
        
        // 去除空格
        functions.put("trim", obj -> {
            if (obj == null) return null;
            return obj.toString().trim();
        });
        
        // 当前日期时间
        functions.put("currentDate", obj -> {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        });
        
        // 字符串长度
        functions.put("length", obj -> {
            if (obj == null) return 0;
            return obj.toString().length();
        });
        
        // 转整数
        functions.put("toInt", obj -> {
            if (obj == null) return 0;
            try {
                return Integer.parseInt(obj.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        });
        
        // 转字符串
        functions.put("toString", obj -> {
            if (obj == null) return "";
            return obj.toString();
        });
        
        // 转浮点数
        functions.put("toDouble", obj -> {
            if (obj == null) return 0.0;
            try {
                return Double.parseDouble(obj.toString());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        });
    }
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        String functionName = (String) ruleConfig.get("function");
        if (functionName == null || functionName.isEmpty()) {
            log.warn("函数名称为空，返回原值");
            return sourceValue;
        }
        
        Function<Object, Object> function = functions.get(functionName);
        if (function == null) {
            log.warn("函数不存在: {}, 返回原值", functionName);
            return sourceValue;
        }
        
        try {
            return function.apply(sourceValue);
        } catch (Exception e) {
            log.error("函数执行失败: {}", e.getMessage(), e);
            return sourceValue;
        }
    }
    
    @Override
    public String getType() {
        return "FUNCTION";
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> ruleConfig) {
        return ruleConfig != null && ruleConfig.containsKey("function");
    }
    
    /**
     * 注册自定义函数
     */
    public void registerFunction(String name, Function<Object, Object> function) {
        functions.put(name, function);
    }
    
    /**
     * 获取所有可用函数名
     */
    public Map<String, String> getAvailableFunctions() {
        Map<String, String> result = new HashMap<>();
        result.put("upperCase", "转大写");
        result.put("lowerCase", "转小写");
        result.put("trim", "去除空格");
        result.put("currentDate", "当前日期时间");
        result.put("length", "字符串长度");
        result.put("toInt", "转整数");
        result.put("toString", "转字符串");
        result.put("toDouble", "转浮点数");
        return result;
    }
}

