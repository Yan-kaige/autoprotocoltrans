package com.kai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM服务类
 * 用于调用大语言模型API分析文档中的报文结构
 * 使用 DeepSeek API
 * 
 * DeepSeek API 兼容 OpenAI 格式
 * 参考文档：https://api-docs.deepseek.com/
 */
@Slf4j
@Service
public class LlmService {
    
    @Value("${llm.api.key:}")
    private String apiKey;
    
    @Value("${llm.api.model:deepseek-chat}")
    private String model;
    
    @Value("${llm.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${llm.api.proxy.enabled:false}")
    private boolean proxyEnabled;
    
    @Value("${llm.api.proxy.host:localhost}")
    private String proxyHost;
    
    @Value("${llm.api.proxy.port:10809}")
    private int proxyPort;
    
    @Value("${llm.api.proxy.type:http}")
    private String proxyType;
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 初始化代理设置
     * 在服务初始化时设置 JVM 级别的代理
     */
    @PostConstruct
    public void initProxy() {
        if (proxyEnabled) {
            // 设置 HTTP 代理
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", String.valueOf(proxyPort));
            
            // 设置 HTTPS 代理
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", String.valueOf(proxyPort));
            
            // 设置代理类型（如果需要）
            if ("socks".equalsIgnoreCase(proxyType)) {
                System.setProperty("socksProxyHost", proxyHost);
                System.setProperty("socksProxyPort", String.valueOf(proxyPort));
            }
            
            log.info("已启用代理: {}://{}:{}", proxyType, proxyHost, proxyPort);
        } else {
            log.debug("代理未启用");
        }
        
        // 初始化 RestTemplate
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
    }
    
    /**
     * 分析文档中的报文结构
     * 
     * @param documentContent 文档内容
     * @return 分析结果列表，每个元素包含协议名称、编码、类型、数据格式等
     */
    public List<Map<String, Object>> analyzeProtocolFromDocument(String documentContent) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DeepSeek API Key未配置，使用默认解析逻辑");
            return Collections.singletonList(analyzeProtocolDefault(documentContent));
        }
        
        try {
            String prompt = buildAnalysisPrompt(documentContent);
            String response = callDeepSeekApi(prompt);
            return parseLlmResponse(response);
        } catch (Exception e) {
            log.error("DeepSeek API分析失败，使用默认解析逻辑: {}", e.getMessage(), e);
            return Collections.singletonList(analyzeProtocolDefault(documentContent));
        }
    }
    
    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String documentContent) {
        return "请分析以下文档中的报文结构，提取出所有协议信息。\n\n" +
               "文档内容：\n" + documentContent + "\n\n" +
               "请以JSON数组格式返回分析结果，每个元素包含以下字段：\n" +
               "[\n" +
               "  {\n" +
               "    \"name\": \"协议名称\",\n" +
               "    \"code\": \"协议编码（英文，驼峰命名）\",\n" +
               "    \"description\": \"协议描述\",\n" +
               "    \"protocolType\": \"JSON或XML\",\n" +
               "    \"category\": \"协议分类\",\n" +
               "    \"dataFormat\": \"完整的数据格式模板（JSON或XML字符串）\"\n" +
               "  }\n" +
               "]\n\n" +
               "如果文档中包含多个不同的协议，请为每个协议创建一个对象。\n" +
               "如果只有一个协议，返回包含一个元素的数组。\n" +
               "如果无法确定协议类型，请根据数据格式判断（包含<标签>的是XML，否则是JSON）。\n" +
               "请直接返回JSON数组，不要包含其他说明文字。";
    }
    
    /**
     * 调用DeepSeek API
     * 
     * DeepSeek API 兼容 OpenAI 格式
     */
    private String callDeepSeekApi(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求体（OpenAI 兼容格式）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", false);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            // 解析响应（OpenAI 兼容格式）
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonNode.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();
                log.info("DeepSeek API响应: {}", content);
                return content;
            } else {
                throw new RuntimeException("DeepSeek API响应格式错误：未找到choices");
            }
            
        } catch (Exception e) {
            log.error("调用DeepSeek API失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用DeepSeek API失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析LLM响应，支持返回多条协议
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseLlmResponse(String response) {
        try {
            // 尝试提取JSON部分（可能包含在markdown代码块中）
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();
            
            // 尝试解析为数组
            Object parsed = objectMapper.readValue(jsonStr, Object.class);
            
            if (parsed instanceof List) {
                // 如果是数组，直接返回
                List<Object> list = (List<Object>) parsed;
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        result.add((Map<String, Object>) item);
                    }
                }
                return result;
            } else if (parsed instanceof Map) {
                // 如果是单个对象，包装成数组
                return Collections.singletonList((Map<String, Object>) parsed);
            } else {
                throw new RuntimeException("无法解析LLM响应：期望JSON数组或对象");
            }
        } catch (Exception e) {
            log.error("解析DeepSeek响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析DeepSeek响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 默认解析逻辑（当LLM不可用时使用）
     */
    private Map<String, Object> analyzeProtocolDefault(String documentContent) {
        Map<String, Object> result = new HashMap<>();
        
        // 尝试提取JSON或XML
        String jsonMatch = extractJson(documentContent);
        String xmlMatch = extractXml(documentContent);
        
        if (jsonMatch != null) {
            result.put("protocolType", "JSON");
            result.put("dataFormat", jsonMatch);
        } else if (xmlMatch != null) {
            result.put("protocolType", "XML");
            result.put("dataFormat", xmlMatch);
        } else {
            result.put("protocolType", "JSON");
            result.put("dataFormat", "{}");
        }
        
        // 生成默认名称和编码
        result.put("name", "从文档导入的协议");
        result.put("code", "imported_protocol_" + System.currentTimeMillis());
        result.put("description", "从文档自动导入");
        result.put("category", "导入协议");
        
        return result;
    }
    
    /**
     * 从文档中提取JSON
     */
    private String extractJson(String content) {
        // 查找JSON对象或数组
        int start = content.indexOf("{");
        if (start == -1) {
            start = content.indexOf("[");
        }
        if (start == -1) {
            return null;
        }
        
        int depth = 0;
        boolean inString = false;
        char startChar = content.charAt(start);
        char endChar = startChar == '{' ? '}' : ']';
        
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == startChar) {
                    depth++;
                } else if (c == endChar) {
                    depth--;
                    if (depth == 0) {
                        return content.substring(start, i + 1);
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从文档中提取XML
     */
    private String extractXml(String content) {
        int start = content.indexOf("<");
        if (start == -1) {
            return null;
        }
        
        // 查找根标签
        int tagEnd = content.indexOf(">", start);
        if (tagEnd == -1) {
            return null;
        }
        
        String rootTag = content.substring(start + 1, tagEnd).split("\\s+")[0];
        if (rootTag.isEmpty()) {
            return null;
        }
        
        // 查找结束标签
        String endTag = "</" + rootTag + ">";
        int end = content.indexOf(endTag, tagEnd);
        if (end == -1) {
            return null;
        }
        
        return content.substring(start, end + endTag.length());
    }
}
