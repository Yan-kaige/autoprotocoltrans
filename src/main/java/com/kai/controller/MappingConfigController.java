package com.kai.controller;

import com.kai.enums.TransactionType;
import com.kai.model.MappingConfig;
import com.kai.model.MappingConfigEntity;
import com.kai.service.MappingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 映射配置管理控制器
 */
@RestController
@RequestMapping("/api/v2/config")
@CrossOrigin(origins = "*")
public class MappingConfigController {
    
    @Autowired
    private MappingConfigService configService;
    
    /**
     * 获取所有配置列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MappingConfigEntity> configs = configService.getAllConfigs();
            result.put("success", true);
            result.put("data", configs);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据ID获取配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConfigById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            MappingConfigEntity entity = configService.getConfigById(id);
            result.put("success", true);
            result.put("data", entity);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据名称获取配置
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Map<String, Object>> getConfigByName(@PathVariable String name) {
        Map<String, Object> result = new HashMap<>();
        try {
            MappingConfig config = configService.getConfigByName(name);
            result.put("success", true);
            result.put("data", config);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存配置
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody SaveConfigRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            MappingConfigEntity entity = configService.saveConfig(
                request.getId(), // 传递ID，如果为null则表示新建
                request.getName(),
                request.getDescription(),
                request.getBankCategory(),
                request.getTransactionName(),
                request.getRequestType(),
                request.getMappingConfig()
            );
            result.put("success", true);
            result.put("data", entity);
            result.put("message", request.getId() != null ? "配置修改成功" : "配置保存成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取标准交易类型列表
     */
    @GetMapping("/transaction-types")
    public ResponseEntity<Map<String, Object>> getTransactionTypes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> types = Arrays.stream(TransactionType.values())
                .map(type -> {
                    Map<String, String> typeMap = new HashMap<>();
                    typeMap.put("value", type.getDisplayName());
                    typeMap.put("label", type.getDisplayName());
                    return typeMap;
                })
                .collect(Collectors.toList());
            result.put("success", true);
            result.put("data", types);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            configService.deleteConfigById(id);
            result.put("success", true);
            result.put("message", "配置删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查配置名称是否存在
     */
    @GetMapping("/check-name/{name}")
    public ResponseEntity<Map<String, Object>> checkNameExists(@PathVariable String name) {
        Map<String, Object> result = new HashMap<>();
        boolean exists = configService.existsByName(name);
        result.put("exists", exists);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存配置请求DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveConfigRequest {
        private Long id; // 配置ID，如果为null则表示新建，如果不为null则表示更新
        private String name;
        private String description;
        private String bankCategory; // 银行类别
        private String transactionName; // 交易名称
        private String requestType; // 请求类型
        private MappingConfig mappingConfig;
    }
}

