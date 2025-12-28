package com.kai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 自定义函数实体类
 */
@TableName("custom_function")
@Data
public class CustomFunction {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 函数名称（唯一）
     */
    private String name;
    
    /**
     * 函数编码（唯一）
     */
    private String code;
    
    /**
     * 函数描述
     */
    private String description;
    
    /**
     * Groovy脚本代码
     * 脚本应该定义一个函数，参数名为 input，返回值作为转换结果
     * 示例：return input.toString().toUpperCase()
     */
    private String script;
    
    /**
     * 是否启用
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





