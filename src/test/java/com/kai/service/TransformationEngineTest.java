package com.kai.service;

import com.kai.enums.MappingType;
import com.kai.enums.TransformType;
import com.kai.model.MappingConfig;
import com.kai.model.MappingRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 转换引擎测试类
 */
@SpringBootTest
class TransformationEngineTest {
    
    @Autowired
    private TransformationEngine transformationEngine;
    
    private String sourceJson;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        sourceJson = """
            {
              "user": {
                "name": "张三",
                "email": "zhangsan@example.com",
                "status": "active",
                "firstName": "张",
                "lastName": "三",
                "age": 25
              },
              "order": {
                "status": "pending"
              }
            }
            """;
    }
    
    @Test
    void testDirectTransform() throws Exception {
        // 测试直接赋值
        MappingConfig config = new MappingConfig();
        config.setSourceProtocol("JSON");
        config.setTargetProtocol("JSON");
        config.setPrettyPrint(true);
        
        List<MappingRule> rules = new ArrayList<>();
        
        MappingRule rule1 = new MappingRule();
        rule1.setSourcePath("$.user.name");
        rule1.setTargetPath("customer.userName");
        rule1.setMappingType(MappingType.ONE_TO_ONE);
        rule1.setTransformType(TransformType.DIRECT);
        rules.add(rule1);
        
        config.setRules(rules);
        
        String result = transformationEngine.transform(sourceJson, config);
        System.out.println("直接转换结果:\n" + result);
        
        assertNotNull(result);
        assertTrue(result.contains("customer"));
        assertTrue(result.contains("userName"));
    }
    
    @Test
    void testFunctionTransform() throws Exception {
        // 测试函数转换
        MappingConfig config = new MappingConfig();
        config.setSourceProtocol("JSON");
        config.setTargetProtocol("JSON");
        
        List<MappingRule> rules = new ArrayList<>();
        
        MappingRule rule = new MappingRule();
        rule.setSourcePath("$.user.status");
        rule.setTargetPath("customer.status");
        rule.setMappingType(MappingType.ONE_TO_ONE);
        rule.setTransformType(TransformType.FUNCTION);
        
        Map<String, Object> transformConfig = new HashMap<>();
        transformConfig.put("function", "upperCase");
        rule.setTransformConfig(transformConfig);
        rules.add(rule);
        
        config.setRules(rules);
        
        String result = transformationEngine.transform(sourceJson, config);
        System.out.println("函数转换结果:\n" + result);
        
        assertNotNull(result);
        assertTrue(result.contains("ACTIVE") || result.contains("\"ACTIVE\""));
    }
    
    @Test
    void testGroovyTransform() throws Exception {
        // 测试Groovy脚本转换（多对1）
        MappingConfig config = new MappingConfig();
        config.setSourceProtocol("JSON");
        config.setTargetProtocol("JSON");
        
        List<MappingRule> rules = new ArrayList<>();
        
        MappingRule rule = new MappingRule();
        rule.setSourcePath("$.user.firstName");
        rule.setTargetPath("customer.fullName");
        rule.setMappingType(MappingType.MANY_TO_ONE);
        rule.setTransformType(TransformType.GROOVY);
        
        // 添加额外源
        rule.setAdditionalSources(Arrays.asList("$.user.lastName"));
        
        Map<String, Object> transformConfig = new HashMap<>();
        transformConfig.put("groovyScript", 
            "def parts = input as List; " +
            "if (parts == null || parts.size() < 2) return ''; " +
            "return (parts[0] ?: '') + ' ' + (parts[1] ?: '')");
        rule.setTransformConfig(transformConfig);
        rules.add(rule);
        
        config.setRules(rules);
        
        String result = transformationEngine.transform(sourceJson, config);
        System.out.println("Groovy转换结果:\n" + result);
        
        assertNotNull(result);
        assertTrue(result.contains("fullName"));
    }
    
    @Test
    void testDictionaryTransform() throws Exception {
        // 测试字典映射
        MappingConfig config = new MappingConfig();
        config.setSourceProtocol("JSON");
        config.setTargetProtocol("JSON");
        
        List<MappingRule> rules = new ArrayList<>();
        
        MappingRule rule = new MappingRule();
        rule.setSourcePath("$.order.status");
        rule.setTargetPath("order.status");
        rule.setMappingType(MappingType.ONE_TO_ONE);
        rule.setTransformType(TransformType.DICTIONARY);
        
        Map<String, Object> transformConfig = new HashMap<>();
        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("pending", "待处理");
        dictionary.put("completed", "已完成");
        dictionary.put("cancelled", "已取消");
        transformConfig.put("dictionary", dictionary);
        rule.setTransformConfig(transformConfig);
        rules.add(rule);
        
        config.setRules(rules);
        
        String result = transformationEngine.transform(sourceJson, config);
        System.out.println("字典转换结果:\n" + result);
        
        assertNotNull(result);
        assertTrue(result.contains("待处理"));
    }
    
    @Test
    void testFixedValueTransform() throws Exception {
        // 测试固定值
        MappingConfig config = new MappingConfig();
        config.setSourceProtocol("JSON");
        config.setTargetProtocol("JSON");
        
        List<MappingRule> rules = new ArrayList<>();
        
        MappingRule rule = new MappingRule();
        rule.setSourcePath(null);
        rule.setTargetPath("order.createTime");
        rule.setMappingType(MappingType.ONE_TO_ONE);
        rule.setTransformType(TransformType.FIXED);
        
        Map<String, Object> transformConfig = new HashMap<>();
        transformConfig.put("fixedValue", "2024-01-01 00:00:00");
        rule.setTransformConfig(transformConfig);
        rules.add(rule);
        
        config.setRules(rules);
        
        String result = transformationEngine.transform(sourceJson, config);
        System.out.println("固定值转换结果:\n" + result);
        
        assertNotNull(result);
        assertTrue(result.contains("2024-01-01 00:00:00"));
    }
    
    @Test
    void testComplexTransform() throws Exception {
        // 测试复合转换（包含多种转换类型）
        MappingConfig config = new MappingConfig();
        config.setSourceProtocol("JSON");
        config.setTargetProtocol("JSON");
        config.setPrettyPrint(true);
        
        List<MappingRule> rules = new ArrayList<>();
        
        // 直接赋值
        MappingRule rule1 = new MappingRule();
        rule1.setSourcePath("$.user.name");
        rule1.setTargetPath("customer.userName");
        rule1.setMappingType(MappingType.ONE_TO_ONE);
        rule1.setTransformType(TransformType.DIRECT);
        rules.add(rule1);
        
        // 函数转换
        MappingRule rule2 = new MappingRule();
        rule2.setSourcePath("$.user.status");
        rule2.setTargetPath("customer.status");
        rule2.setMappingType(MappingType.ONE_TO_ONE);
        rule2.setTransformType(TransformType.FUNCTION);
        Map<String, Object> config2 = new HashMap<>();
        config2.put("function", "upperCase");
        rule2.setTransformConfig(config2);
        rules.add(rule2);
        
        // Groovy脚本（多对1）
        MappingRule rule3 = new MappingRule();
        rule3.setSourcePath("$.user.firstName");
        rule3.setTargetPath("customer.fullName");
        rule3.setMappingType(MappingType.MANY_TO_ONE);
        rule3.setTransformType(TransformType.GROOVY);
        rule3.setAdditionalSources(Arrays.asList("$.user.lastName"));
        Map<String, Object> config3 = new HashMap<>();
        config3.put("groovyScript", 
            "def parts = input as List; " +
            "if (parts == null || parts.size() < 2) return ''; " +
            "return (parts[0] ?: '') + ' ' + (parts[1] ?: '')");
        rule3.setTransformConfig(config3);
        rules.add(rule3);
        
        config.setRules(rules);
        
        String result = transformationEngine.transform(sourceJson, config);
        System.out.println("复合转换结果:\n" + result);
        
        assertNotNull(result);
        assertTrue(result.contains("customer"));
    }
}

