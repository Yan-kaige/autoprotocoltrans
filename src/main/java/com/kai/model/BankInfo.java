package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 银行信息实体类
 */
@TableName("bank_info")
@Data
public class BankInfo {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 银行名称（唯一）
     */
    private String name;
    
    /**
     * 银行编码（唯一）
     */
    private String code;
    
    /**
     * 银行描述
     */
    private String description;
    
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

