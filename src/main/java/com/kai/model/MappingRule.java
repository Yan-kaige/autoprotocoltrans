package com.kai.model;

import com.kai.enums.MappingType;
import com.kai.enums.TransformType;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 字段映射规则
 * 用于描述源字段到目标字段的转换规则
 */
@Data
public class MappingRule {
    /**
     * 源字段路径（JsonPath表达式）
     * 例如：$.user.name, $.order.items[*].id
     * 对于多对1映射，可能有多个sourcePath
     */
    private String sourcePath;
    
    /**
     * 额外的源路径（用于多对1映射）
     * 例如：firstName + lastName -> fullName
     */
    private List<String> additionalSources;
    
    /**
     * 目标字段路径（支持深度路径，如 user.address.city）
     */
    private String targetPath;
    
    /**
     * 映射类型
     * ONE_TO_ONE: 1对1
     * ONE_TO_MANY: 1对多
     * MANY_TO_ONE: 多对1
     */
    private MappingType mappingType;
    
    /**
     * 转换类型
     * DIRECT, FUNCTION, GROOVY, DICTIONARY, FIXED, IGNORE
     */
    private TransformType transformType;
    
    /**
     * 转换规则配置（根据不同的transformType，配置不同）
     * 
     * DIRECT: 无额外配置
     * FUNCTION: {"function": "upperCase"}
     * GROOVY: {"groovyScript": "return input.toUpperCase()"}
     * DICTIONARY: {"dictionary": {"key1": "value1", "key2": "value2"}}
     * FIXED: {"fixedValue": "固定值"}
     */
    private Map<String, Object> transformConfig;
}

