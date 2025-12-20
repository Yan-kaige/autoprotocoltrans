package com.kai.model;

import lombok.Data;
import java.util.List;

/**
 * 完整的映射配置
 * 前端传递的配置结构，包含源协议、目标协议和所有映射规则
 */
@Data
public class MappingConfig {
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
}

