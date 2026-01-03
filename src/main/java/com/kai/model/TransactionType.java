package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 交易类型实体类
 */
@TableName("transaction_type")
@Data
public class TransactionType {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 银行ID
     */
    private Long bankId;
    
    /**
     * 交易类型名称（如：今日余额查询、历史余额查询等）
     */
    private String transactionName;
    
    /**
     * 交易类型描述
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


