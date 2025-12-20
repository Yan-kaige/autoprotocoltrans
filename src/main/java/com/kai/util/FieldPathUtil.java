package com.kai.util;

import java.util.Map;

/**
 * 字段路径工具类
 * 支持嵌套路径访问，如 user.name 或 user.address.city
 */
public class FieldPathUtil {
    
    /**
     * 根据路径获取值
     */
    public static Object getValueByPath(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return data;
        }
        
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 根据路径设置值
     */
    @SuppressWarnings("unchecked")
    public static void setValueByPath(Map<String, Object> data, String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        
        // 创建嵌套路径，除了最后一个
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            
            if (next == null || !(next instanceof Map)) {
                next = new java.util.HashMap<>();
                current.put(part, next);
            }
            
            current = (Map<String, Object>) next;
        }
        
        // 设置最后的值
        current.put(parts[parts.length - 1], value);
    }
    
    /**
     * 检查路径是否存在
     */
    public static boolean pathExists(Map<String, Object> data, String path) {
        return getValueByPath(data, path) != null;
    }
}

