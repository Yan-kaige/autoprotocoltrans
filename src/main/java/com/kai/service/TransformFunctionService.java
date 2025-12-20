package com.kai.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 转换函数服务
 * 提供预置的转换函数
 */
@Service
public class TransformFunctionService {
    
    private final Map<String, Function<Object, Object>> functions = new HashMap<>();
    
    public TransformFunctionService() {
        initDefaultFunctions();
    }
    
    /**
     * 初始化默认函数
     */
    private void initDefaultFunctions() {
        // 字符串转大写
        registerFunction("upperCase", obj -> {
            if (obj == null) return null;
            return obj.toString().toUpperCase();
        });
        
        // 字符串转小写
        registerFunction("lowerCase", obj -> {
            if (obj == null) return null;
            return obj.toString().toLowerCase();
        });
        
        // 去除空格
        registerFunction("trim", obj -> {
            if (obj == null) return null;
            return obj.toString().trim();
        });
        
        // 日期格式化（当前时间）
        registerFunction("currentDate", obj -> {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        });
        
        // 字符串长度
        registerFunction("length", obj -> {
            if (obj == null) return 0;
            return obj.toString().length();
        });
        
        // 转整数
        registerFunction("toInt", obj -> {
            if (obj == null) return 0;
            try {
                return Integer.parseInt(obj.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        });
        
        // 转字符串
        registerFunction("toString", obj -> {
            if (obj == null) return "";
            return obj.toString();
        });
        
        // 转浮点数
        registerFunction("toDouble", obj -> {
            if (obj == null) return 0.0;
            try {
                return Double.parseDouble(obj.toString());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        });
    }
    
    /**
     * 注册自定义函数
     */
    public void registerFunction(String name, Function<Object, Object> function) {
        functions.put(name, function);
    }
    
    /**
     * 执行函数
     */
    public Object executeFunction(String functionName, Object input) {
        Function<Object, Object> function = functions.get(functionName);
        if (function == null) {
            throw new IllegalArgumentException("函数不存在: " + functionName);
        }
        return function.apply(input);
    }
    
    /**
     * 获取所有可用函数
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

