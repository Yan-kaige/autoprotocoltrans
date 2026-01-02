package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 映射配置实体类（新结构）
 */
@TableName("mapping_config_v2")
@Data
public class MappingConfigV2 {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 交易类型ID
     */
    private Long transactionTypeId;
    
    /**
     * 配置类型：REQUEST（请求）或 RESPONSE（响应）
     */
    private String configType;
    
    /**
     * 配置版本（如：v1、v2等）
     */
    private String version;
    
    /**
     * 配置名称
     */
    private String name;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 配置内容（JSON格式的MappingConfig）
     */
    private String configContent;
    
    /**
     * 是否启用（1:启用，0:禁用）
     */
    private Boolean enabled;
    
    /**
     * 是否为当前版本（1:是，0:否）
     */
    private Boolean isCurrent;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

