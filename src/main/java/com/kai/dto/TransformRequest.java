package com.kai.dto;

import lombok.Data;

/**
 * 转换请求DTO
 */
@Data
public class TransformRequest {
    /**
     * 规则ID
     */
    private String ruleId;
    
    /**
     * 源数据（JSON或XML字符串）
     */
    private String sourceData;
    
    /**
     * 是否格式化输出
     */
    private Boolean prettyPrint = false;
}

