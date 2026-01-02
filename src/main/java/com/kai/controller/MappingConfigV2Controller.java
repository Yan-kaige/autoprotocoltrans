package com.kai.controller;

import com.kai.enums.TransactionType;
import com.kai.model.MappingConfig;
import com.kai.model.MappingConfigV2;
import com.kai.service.MappingConfigV2Service;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 映射配置管理控制器（新结构）
 */
@RestController
@RequestMapping("/api/v2/config")
@CrossOrigin(origins = "*")
@Slf4j
public class MappingConfigV2Controller {
    
    @Autowired
    private MappingConfigV2Service configService;
    
    /**
     * 保存配置
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody SaveConfigRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            MappingConfigV2 entity;
            
            // 如果选择了创建新版本
            if (request.getCreateNewVersion() != null && request.getCreateNewVersion() && request.getId() != null) {
                // 创建新版本
                String newVersion = configService.createNewVersion(
                    request.getTransactionTypeId(),
                    request.getMappingConfig().getConfigType(),
                    objectMapper.writeValueAsString(request.getMappingConfig())
                );
                result.put("success", true);
                result.put("data", null);
                result.put("message", "新版本创建成功: " + newVersion);
                result.put("newVersion", newVersion);
            } else {
                // 正常保存或更新
                entity = configService.saveConfig(
                    request.getId(),
                    request.getTransactionTypeId(),
                    request.getMappingConfig().getConfigType(),
                    request.getVersion(),
                    request.getName(),
                    request.getDescription(),
                    request.getMappingConfig()
                );
                result.put("success", true);
                result.put("data", entity);
                result.put("message", request.getId() != null ? "配置修改成功" : "配置保存成功");
            }
        } catch (Exception e) {
            log.error("保存配置失败", e);
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
            MappingConfigV2 entity = configService.getConfigById(id);
            result.put("success", true);
            result.put("data", entity);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取当前版本的配置
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentConfig(
            @RequestParam Long transactionTypeId,
            @RequestParam String configType) {
        Map<String, Object> result = new HashMap<>();
        try {
            MappingConfigV2 config = configService.getCurrentConfig(transactionTypeId, configType);
            result.put("success", true);
            result.put("data", config);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取指定版本的配置
     */
    @GetMapping("/by-version")
    public ResponseEntity<Map<String, Object>> getConfigByVersion(
            @RequestParam Long transactionTypeId,
            @RequestParam String configType,
            @RequestParam String version) {
        Map<String, Object> result = new HashMap<>();
        try {
            MappingConfigV2 config = configService.getConfigByVersion(transactionTypeId, configType, version);
            result.put("success", true);
            result.put("data", config);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取所有版本列表
     */
    @GetMapping("/versions")
    public ResponseEntity<Map<String, Object>> getVersions(@RequestParam Long transactionTypeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> versions = configService.getVersions(transactionTypeId);
            result.put("success", true);
            result.put("data", versions);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 回退到指定版本
     */
    @PostMapping("/rollback")
    public ResponseEntity<Map<String, Object>> rollbackToVersion(@RequestBody RollbackRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = configService.rollbackToVersion(
                    request.getTransactionTypeId(),
                    request.getVersion()
            );
            result.put("success", success);
            result.put("message", success ? "版本回退成功" : "版本回退失败");
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
     * 保存配置请求DTO
     */
    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveConfigRequest {
        private Long id;
        private Long transactionTypeId;
        private String version;
        private String name;
        private String description;
        private Boolean createNewVersion;
        private MappingConfig mappingConfig;
    }
    
    /**
     * 版本回退请求DTO
     */
    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RollbackRequest {
        private Long transactionTypeId;
        private String version;
    }
    
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    
    /**
     * 获取标准交易类型列表（枚举值）
     */
    @GetMapping("/transaction-types")
    public ResponseEntity<Map<String, Object>> getTransactionTypes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> types = Arrays.stream(TransactionType.values())
                    .map(type -> {
                        Map<String, String> typeMap = new HashMap<>();
                        typeMap.put("value", type.name());
                        typeMap.put("label", type.getDisplayName());
                        return typeMap;
                    })
                    .collect(Collectors.toList());
            result.put("success", true);
            result.put("data", types);
        } catch (Exception e) {
            log.error("获取交易类型列表失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}

