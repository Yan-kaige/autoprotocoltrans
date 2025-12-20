package com.kai.controller;

import com.kai.dto.TransformRequest;
import com.kai.dto.TransformResponse;
import com.kai.model.TransformRule;
import com.kai.service.TransformRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 转换规则控制器
 */
@RestController
@RequestMapping("/api/rules")
@CrossOrigin(origins = "*")
public class TransformRuleController {
    
    @Autowired
    private TransformRuleService ruleService;
    
    /**
     * 获取所有规则
     */
    @GetMapping
    public ResponseEntity<List<TransformRule>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }
    
    /**
     * 根据ID获取规则
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransformRule> getRuleById(@PathVariable String id) {
        TransformRule rule = ruleService.getRuleById(id);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }
    
    /**
     * 创建或更新规则
     */
    @PostMapping
    public ResponseEntity<TransformRule> saveRule(@RequestBody TransformRule rule) {
        return ResponseEntity.ok(ruleService.saveRule(rule));
    }
    
    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        boolean deleted = ruleService.deleteRule(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 执行转换
     */
    @PostMapping("/transform")
    public ResponseEntity<TransformResponse> transform(@RequestBody TransformRequest request) {
        TransformResponse response = new TransformResponse();
        try {
            String result = ruleService.executeTransform(
                request.getRuleId(),
                request.getSourceData(),
                request.getPrettyPrint() != null ? request.getPrettyPrint() : false
            );
            response.setSuccess(true);
            response.setTransformedData(result);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}

