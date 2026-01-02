package com.kai.controller;

import com.kai.model.BankInfo;
import com.kai.service.BankInfoService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 银行信息管理控制器
 */
@RestController
@RequestMapping("/api/v2/bank")
@CrossOrigin(origins = "*")
@Slf4j
public class BankInfoController {
    
    @Autowired
    private BankInfoService bankInfoService;
    
    /**
     * 获取所有银行信息列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllBanks() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<BankInfo> banks = bankInfoService.list();
            result.put("success", true);
            result.put("data", banks);
        } catch (Exception e) {
            log.error("获取银行列表失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取所有启用的银行信息列表
     */
    @GetMapping("/list/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledBanks() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<BankInfo> banks = bankInfoService.getEnabledBanks();
            result.put("success", true);
            result.put("data", banks);
        } catch (Exception e) {
            log.error("获取启用的银行列表失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据ID获取银行信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBankById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            BankInfo bank = bankInfoService.getById(id);
            if (bank == null) {
                result.put("success", false);
                result.put("errorMessage", "银行信息不存在");
            } else {
                result.put("success", true);
                result.put("data", bank);
            }
        } catch (Exception e) {
            log.error("获取银行信息失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存银行信息（新增或更新）
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveBank(@RequestBody SaveBankRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            BankInfo bank = bankInfoService.saveBankInfo(
                    request.getId(),
                    request.getName(),
                    request.getCode(),
                    request.getDescription(),
                    request.getEnabled()
            );
            result.put("success", true);
            result.put("data", bank);
            result.put("message", request.getId() != null ? "银行信息更新成功" : "银行信息保存成功");
        } catch (Exception e) {
            log.error("保存银行信息失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除银行信息
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBank(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = bankInfoService.deleteBankInfo(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除银行信息失败", e);
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
                    ? bankInfoService.existsByCodeExcludingId(code, excludeId)
                    : bankInfoService.existsByCode(code);
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
     * 保存银行信息请求DTO
     */
    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveBankRequest {
        private Long id;
        private String name;
        private String code;
        private String description;
        private Boolean enabled;
    }
}

