package com.kai.model;

import com.kai.enums.ProtocolType;
import lombok.Data;

import java.util.List;

/**
 * 转换规则配置
 */
@Data
public class TransformRule {
    /**
     * 规则ID
     */
    private String id;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 源协议类型
     */
    private ProtocolType sourceType;
    
    /**
     * 目标协议类型
     */
    private ProtocolType targetType;
    
    /**
     * 字段映射列表
     */
    private List<FieldMapping> fieldMappings;
    
    /**
     * 是否启用
     */
    private Boolean enabled = true;
}

