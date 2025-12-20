package com.kai.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路径工具类
 * 用于处理深度路径的创建和赋值
 * 例如：user.address.city -> 自动创建嵌套的Map结构
 */
public class PathUtil {
    
    /**
     * 根据深度路径设置值
     * 如果路径不存在，会自动创建中间节点
     * 
     * @param targetMap 目标Map
     * @param path 路径，使用点号分隔，如 "user.address.city"
     * @param value 要设置的值
     */
    @SuppressWarnings("unchecked")
    public static void setDeepValue(Map<String, Object> targetMap, String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = targetMap;
        
        // 创建嵌套路径，除了最后一个
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            
            if (next == null || !(next instanceof Map)) {
                next = new HashMap<>();
                current.put(part, next);
            }
            
            current = (Map<String, Object>) next;
        }
        
        // 设置最后的值
        current.put(parts[parts.length - 1], value);
    }
    
    /**
     * 根据深度路径获取值
     * 
     * @param sourceMap 源Map
     * @param path 路径，使用点号分隔
     * @return 值，如果路径不存在返回null
     */
    @SuppressWarnings("unchecked")
    public static Object getDeepValue(Map<String, Object> sourceMap, String path) {
        if (path == null || path.isEmpty()) {
            return sourceMap;
        }
        
        String[] parts = path.split("\\.");
        Object current = sourceMap;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 检查路径是否存在
     */
    public static boolean pathExists(Map<String, Object> sourceMap, String path) {
        return getDeepValue(sourceMap, path) != null;
    }
    
    /**
     * 将JsonPath表达式转换为普通路径（简单处理）
     * 例如：$.user.name -> user.name
     * 注意：这是简化处理，复杂的JsonPath表达式需要更复杂的解析
     */
    public static String jsonPathToSimplePath(String jsonPath) {
        if (jsonPath == null) {
            return null;
        }
        
        // 移除 $.
        if (jsonPath.startsWith("$.")) {
            return jsonPath.substring(2);
        }
        // 移除 $
        if (jsonPath.startsWith("$")) {
            return jsonPath.substring(1);
        }
        
        return jsonPath;
    }
}

