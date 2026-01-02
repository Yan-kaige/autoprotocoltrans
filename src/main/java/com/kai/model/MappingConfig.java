package com.kai.model;

import lombok.Data;
import java.util.List;

/**
 * 完整的映射配置
 * 前端传递的配置结构，包含源协议、目标协议和所有映射规则
 * 支持请求和响应两种类型的配置
 */
@Data
public class MappingConfig {
    /**
     * 配置类型：REQUEST（请求：标准请求->其他银行请求）或 RESPONSE（响应：其他银行响应->标准响应）
     */
    private String configType;
    
    /**
     * 源协议类型：JSON 或 XML
     */
    private String sourceProtocol;
    
    /**
     * 目标协议类型：JSON 或 XML
     */
    private String targetProtocol;
    
    /**
     * 映射规则列表
     */
    private List<MappingRule> rules;
    
    /**
     * 是否格式化输出（JSON格式化、XML缩进）
     */
    private Boolean prettyPrint = false;
    
    /**
     * XML根元素名称（当targetProtocol为XML时使用）
     */
    private String xmlRootElementName;
    
    /**
     * 是否包含XML声明（当targetProtocol为XML时使用）
     */
    private Boolean includeXmlDeclaration = false;
}

