package com.kai.controller;

import com.kai.model.TransactionType;
import com.kai.service.TransactionTypeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易类型管理控制器
 */
@RestController
@RequestMapping("/api/v2/transaction-type")
@CrossOrigin(origins = "*")
@Slf4j
public class TransactionTypeController {
    
    @Autowired
    private TransactionTypeService transactionTypeService;
    
    /**
     * 根据银行ID获取所有交易类型
     */
    @GetMapping("/by-bank/{bankId}")
    public ResponseEntity<Map<String, Object>> getByBankId(@PathVariable Long bankId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<TransactionType> transactionTypes = transactionTypeService.getByBankId(bankId);
            result.put("success", true);
            result.put("data", transactionTypes);
        } catch (Exception e) {
            log.error("获取交易类型列表失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据ID获取交易类型
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            TransactionType transactionType = transactionTypeService.getById(id);
            if (transactionType == null) {
                result.put("success", false);
                result.put("errorMessage", "交易类型不存在");
            } else {
                result.put("success", true);
                result.put("data", transactionType);
            }
        } catch (Exception e) {
            log.error("获取交易类型失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存交易类型（新增或更新）
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveTransactionType(@RequestBody SaveTransactionTypeRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            TransactionType transactionType = transactionTypeService.saveTransactionType(
                    request.getId(),
                    request.getBankId(),
                    request.getTransactionName(),
                    request.getDescription(),
                    request.getEnabled()
            );
            result.put("success", true);
            result.put("data", transactionType);
            result.put("message", request.getId() != null ? "交易类型更新成功" : "交易类型保存成功");
        } catch (Exception e) {
            log.error("保存交易类型失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除交易类型
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTransactionType(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = transactionTypeService.deleteTransactionType(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除交易类型失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存交易类型请求DTO
     */
    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveTransactionTypeRequest {
        private Long id;
        private Long bankId;
        private String transactionName;
        private String description;
        private Boolean enabled;
    }
}


