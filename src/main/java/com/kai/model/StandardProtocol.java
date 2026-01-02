package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 标准协议实体类
 */
@TableName("standard_protocol")
@Data
public class StandardProtocol {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 协议名称（唯一）
     */
    private String name;
    
    /**
     * 协议编码（唯一）
     */
    private String code;
    
    /**
     * 协议描述
     */
    private String description;
    
    /**
     * 协议类型（JSON/XML）
     */
    private String protocolType;
    
    /**
     * 数据格式模板（JSON或XML字符串）
     */
    private String dataFormat;
    
    /**
     * 协议分类（如：用户信息、订单信息、支付信息等）
     */
    private String category;
    
    /**
     * 是否启用（1:启用，0:禁用）
     */
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}





