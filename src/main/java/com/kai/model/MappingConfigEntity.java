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

