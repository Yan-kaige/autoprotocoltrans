package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 映射配置实体类（数据库存储）
 */
@TableName("mapping_config")
@Data
public class MappingConfigEntity {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置名称（唯一）
     */
    private String name;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 银行类别
     */
    private String bankCategory;
    
    /**
     * 交易名称
     */
    private String transactionName;
    
    /**
     * 请求类型（如：今日余额查询、历史余额查询等）
     */
    private String requestType;
    
    /**
     * 是否启用（1:启用，0:禁用）
     */
    private Boolean enabled;
    
    /**
     * 配置版本（如：v1、v2等）
     */
    private String version;
    
    /**
     * 配置内容（JSON格式的MappingConfig）
     */
    private String configContent;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

