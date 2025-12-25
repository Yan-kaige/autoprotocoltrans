package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字典项实体类
 */
@TableName("dictionary_item")
@Data
public class DictionaryItem {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 字典ID（关联dictionary表）
     */
    private Long dictionaryId;
    
    /**
     * 字典键（key）
     */
    private String dictKey;
    
    /**
     * 字典值（value）
     */
    private String dictValue;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

