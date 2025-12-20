package com.kai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.Map;

/**
 * JSON工具类
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

    /**
     * JSON字符串转Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) throws Exception {
        return objectMapper.readValue(json, Map.class);
    }

    /**
     * Map转JSON字符串
     */
    public static String mapToJson(Map<String, Object> map, boolean prettyPrint) throws Exception {
        if (prettyPrint) {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        }
        return objectMapper.writeValueAsString(map);
    }

    /**
     * XML字符串转Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> xmlToMap(String xml) throws Exception {
        return xmlMapper.readValue(xml, Map.class);
    }

    /**
     * Map转XML字符串
     */
    public static String mapToXml(Map<String, Object> map, boolean prettyPrint) throws Exception {
        if (prettyPrint) {
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        }
        return xmlMapper.writeValueAsString(map);
    }

    /**
     * 获取ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

