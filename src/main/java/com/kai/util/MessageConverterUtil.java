package com.kai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 消息转换工具类
 * 统一处理 JSON 和 XML 格式的转换
 * 
 * 功能：
 * 1. XML字符串 -> Map（供引擎处理）
 * 2. Map -> XML字符串（引擎输出）
 * 3. JSON字符串 -> Map（供引擎处理）
 * 4. Map -> JSON字符串（引擎输出）
 */
@Slf4j
public class MessageConverterUtil {
    
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();
    
    /**
     * 将源数据字符串转换为Map（根据协议类型自动识别）
     * 
     * @param sourceData 源数据字符串（JSON或XML）
     * @param sourceType 源数据类型："JSON" 或 "XML"
     * @return Map对象
     * @throws Exception 转换失败时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseToMap(String sourceData, String sourceType) throws Exception {
        if (sourceData == null || sourceData.trim().isEmpty()) {
            throw new IllegalArgumentException("源数据不能为空");
        }
        
        String type = sourceType != null ? sourceType.toUpperCase() : "JSON";
        
        try {
            switch (type) {
                case "XML":
                    return xmlMapper.readValue(sourceData, Map.class);
                case "JSON":
                default:
                    return jsonMapper.readValue(sourceData, Map.class);
            }
        } catch (Exception e) {
            log.error("解析{}数据失败: {}", type, e.getMessage(), e);
            throw new Exception("解析" + type + "数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将Map转换为目标格式字符串
     * 
     * @param targetMap 目标Map
     * @param targetType 目标类型："JSON" 或 "XML"
     * @param prettyPrint 是否格式化输出
     * @return 格式化后的字符串
     * @throws Exception 转换失败时抛出异常
     */
    public static String mapToString(Map<String, Object> targetMap, String targetType, boolean prettyPrint) throws Exception {
        if (targetMap == null) {
            throw new IllegalArgumentException("目标Map不能为空");
        }
        
        String type = targetType != null ? targetType.toUpperCase() : "JSON";
        
        try {
            switch (type) {
                case "XML":
                    if (prettyPrint) {
                        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(targetMap);
                    }
                    return xmlMapper.writeValueAsString(targetMap);
                case "JSON":
                default:
                    if (prettyPrint) {
                        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(targetMap);
                    }
                    return jsonMapper.writeValueAsString(targetMap);
            }
        } catch (Exception e) {
            log.error("转换为{}格式失败: {}", type, e.getMessage(), e);
            throw new Exception("转换为" + type + "格式失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检测字符串是否为XML格式
     * 
     * @param data 数据字符串
     * @return true表示可能是XML，false表示可能是JSON
     */
    public static boolean isXmlFormat(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }
        String trimmed = data.trim();
        return trimmed.startsWith("<") && trimmed.endsWith(">");
    }
    
    /**
     * 获取ObjectMapper实例（JSON）
     */
    public static ObjectMapper getJsonMapper() {
        return jsonMapper;
    }
    
    /**
     * 获取XmlMapper实例（XML）
     */
    public static XmlMapper getXmlMapper() {
        return xmlMapper;
    }
}

