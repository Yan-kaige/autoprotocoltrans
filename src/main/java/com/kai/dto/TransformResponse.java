package com.kai.dto;

import lombok.Data;

/**
 * 转换响应DTO
 */
@Data
public class TransformResponse {
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 转换后的数据
     */
    private String transformedData;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}

