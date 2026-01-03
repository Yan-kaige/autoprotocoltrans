package com.kai.controller;

import com.kai.model.CustomFunction;
import com.kai.service.CustomFunctionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义函数管理控制器
 */
@RestController
@RequestMapping("/api/v2/function")
@Slf4j
public class CustomFunctionController {
    
    @Autowired
    private CustomFunctionService customFunctionService;
    
    /**
     * 获取所有函数列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllFunctions() {
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.List<CustomFunction> functions = customFunctionService.list();
            result.put("success", true);
            result.put("data", functions);
        } catch (Exception e) {
            log.error("获取函数列表失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据ID获取函数
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFunction(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            CustomFunction func = customFunctionService.getById(id);
            if (func == null) {
                result.put("success", false);
                result.put("errorMessage", "函数不存在");
            } else {
                result.put("success", true);
                result.put("data", func);
            }
        } catch (Exception e) {
            log.error("获取函数失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存函数（新增或更新）
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFunction(@RequestBody SaveFunctionRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            CustomFunction function = customFunctionService.saveFunction(
                    request.getId(),
                    request.getName(),
                    request.getCode(),
                    request.getDescription(),
                    request.getScript(),
                    request.getEnabled()
            );
            result.put("success", true);
            result.put("data", function);
            result.put("message", request.getId() != null ? "函数更新成功" : "函数保存成功");
        } catch (Exception e) {
            log.error("保存函数失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除函数
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFunction(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = customFunctionService.removeById(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除函数失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查编码是否存在
     */
    @GetMapping("/check-code")
    public ResponseEntity<Map<String, Object>> checkCodeExists(
            @RequestParam String code,
            @RequestParam(required = false) Long excludeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean exists = excludeId != null 
                    ? customFunctionService.existsByCodeExcludingId(code, excludeId)
                    : customFunctionService.existsByCode(code);
            result.put("success", true);
            result.put("exists", exists);
        } catch (Exception e) {
            log.error("检查编码失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存函数请求DTO
     */
    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveFunctionRequest {
        private Long id;
        private String name;
        private String code;
        private String description;
        private String script;
        private Boolean enabled;
    }
}












