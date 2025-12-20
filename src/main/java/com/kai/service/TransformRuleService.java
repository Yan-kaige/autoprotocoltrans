package com.kai.service;

import com.kai.model.TransformRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 转换规则服务
 * 管理转换规则的CRUD操作
 */
@Service
public class TransformRuleService {
    
    // 使用内存存储规则（实际项目中可以使用数据库）
    private final Map<String, TransformRule> ruleStore = new ConcurrentHashMap<>();
    
    @Autowired
    private TransformEngine transformEngine;
    
    /**
     * 保存规则
     */
    public TransformRule saveRule(TransformRule rule) {
        // 如果规则ID为空，生成新ID
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(UUID.randomUUID().toString());
        }
        // 直接存储规则对象（前端已经做了深拷贝）
        // 如果担心引用问题，可以考虑使用序列化/反序列化创建深拷贝
        ruleStore.put(rule.getId(), rule);
        return rule;
    }
    
    /**
     * 根据ID获取规则
     */
    public TransformRule getRuleById(String id) {
        return ruleStore.get(id);
    }
    
    /**
     * 获取所有规则
     */
    public List<TransformRule> getAllRules() {
        return new ArrayList<>(ruleStore.values());
    }
    
    /**
     * 删除规则
     */
    public boolean deleteRule(String id) {
        return ruleStore.remove(id) != null;
    }
    
    /**
     * 执行转换
     */
    public String executeTransform(String ruleId, String sourceData, boolean prettyPrint) throws Exception {
        TransformRule rule = getRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("规则不存在: " + ruleId);
        }
        if (!rule.getEnabled()) {
            throw new IllegalStateException("规则已禁用: " + ruleId);
        }
        return transformEngine.transform(rule, sourceData, prettyPrint);
    }
}

