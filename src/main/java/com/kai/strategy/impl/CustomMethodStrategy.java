package com.kai.strategy.impl;

import com.kai.strategy.TransformStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 自定义后端方法策略
 * 通过反射调用指定的类和方法进行转换
 * 配置参数：
 * - className: 完整的类名（包含包名），如 "com.kai.util.StringUtil"
 * - methodName: 方法名，如 "toUpperCase"
 * 
 * 方法签名要求：
 * - 方法必须是 public static
 * - 方法接受一个 Object 参数（源值）
 * - 方法返回 Object（转换后的值）
 */
@Slf4j
@Component
public class CustomMethodStrategy implements TransformStrategy {
    
    @Override
    public Object transform(Object sourceValue, Map<String, Object> ruleConfig) {
        if (ruleConfig == null) {
            log.warn("规则配置为空，返回原值");
            return sourceValue;
        }
        
        String className = (String) ruleConfig.get("className");
        String methodName = (String) ruleConfig.get("methodName");
        
        if (className == null || className.trim().isEmpty()) {
            log.warn("类名为空，返回原值");
            return sourceValue;
        }
        
        if (methodName == null || methodName.trim().isEmpty()) {
            log.warn("方法名为空，返回原值");
            return sourceValue;
        }
        
        try {
            // 加载类
            Class<?> clazz = Class.forName(className);
            
            // 查找方法：优先查找接受 Object 参数的静态方法
            Method method = null;
            try {
                // 尝试查找 public static 方法，接受 Object 参数
                method = clazz.getMethod(methodName, Object.class);
            } catch (NoSuchMethodException e) {
                // 如果找不到，尝试查找接受 String 参数的方法
                try {
                    method = clazz.getMethod(methodName, String.class);
                } catch (NoSuchMethodException e2) {
                    // 如果还找不到，尝试查找无参数的方法（可能使用实例变量）
                    try {
                        method = clazz.getMethod(methodName);
                    } catch (NoSuchMethodException e3) {
                        log.error("找不到方法: {}.{}，尝试的参数类型: Object, String, 无参数", className, methodName);
                        return sourceValue;
                    }
                }
            }
            
            // 检查方法是否为静态方法
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                log.warn("方法 {}.{} 不是静态方法，尝试创建实例调用", className, methodName);
                // 如果不是静态方法，创建实例调用
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (method.getParameterCount() == 0) {
                    return method.invoke(instance);
                } else if (method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    if (paramType == Object.class) {
                        return method.invoke(instance, sourceValue);
                    } else if (paramType == String.class) {
                        return method.invoke(instance, sourceValue != null ? sourceValue.toString() : null);
                    } else {
                        // 尝试类型转换
                        return method.invoke(instance, convertType(sourceValue, paramType));
                    }
                } else {
                    log.error("方法 {}.{} 参数数量不正确，期望0或1个参数", className, methodName);
                    return sourceValue;
                }
            }
            
            // 调用静态方法
            if (method.getParameterCount() == 0) {
                return method.invoke(null);
            } else if (method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType == Object.class) {
                    return method.invoke(null, sourceValue);
                } else if (paramType == String.class) {
                    return method.invoke(null, sourceValue != null ? sourceValue.toString() : null);
                } else {
                    // 尝试类型转换
                    return method.invoke(null, convertType(sourceValue, paramType));
                }
            } else {
                log.error("方法 {}.{} 参数数量不正确，期望0或1个参数", className, methodName);
                return sourceValue;
            }
            
        } catch (ClassNotFoundException e) {
            log.error("找不到类: {}, 错误: {}", className, e.getMessage());
            return sourceValue;
        } catch (Exception e) {
            log.error("调用方法 {}.{} 失败: {}", className, methodName, e.getMessage(), e);
            return sourceValue;
        }
    }
    
    /**
     * 类型转换辅助方法
     */
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // 转换为字符串
        String strValue = value.toString();
        
        // 根据目标类型进行转换
        if (targetType == String.class) {
            return strValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (targetType == Long.class || targetType == long.class) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                return 0L;
            }
        } else if (targetType == Double.class || targetType == double.class) {
            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(strValue);
        }
        
        // 如果无法转换，返回原值
        return value;
    }
    
    @Override
    public String getType() {
        return "CUSTOM_METHOD";
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> ruleConfig) {
        if (ruleConfig == null) {
            return false;
        }
        String className = (String) ruleConfig.get("className");
        String methodName = (String) ruleConfig.get("methodName");
        return className != null && !className.trim().isEmpty() 
            && methodName != null && !methodName.trim().isEmpty();
    }
}

