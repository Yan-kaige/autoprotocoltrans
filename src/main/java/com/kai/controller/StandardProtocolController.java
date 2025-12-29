package com.kai.controller;

import com.kai.model.StandardProtocol;
import com.kai.service.StandardProtocolService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准协议控制器
 */
@RestController
@RequestMapping("/api/standard-protocol")
@CrossOrigin(origins = "*")
public class StandardProtocolController {
    
    @Autowired
    private StandardProtocolService protocolService;
    
    /**
     * 获取所有标准协议
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProtocols() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<StandardProtocol> protocols = protocolService.list();
            result.put("success", true);
            result.put("data", protocols);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据协议类型获取启用的协议列表
     */
    @GetMapping("/by-type")
    public ResponseEntity<Map<String, Object>> getProtocolsByType(@RequestParam String protocolType) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<StandardProtocol> protocols = protocolService.getEnabledProtocolsByType(protocolType);
            result.put("success", true);
            result.put("data", protocols);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据ID获取协议
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProtocolById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            StandardProtocol protocol = protocolService.getById(id);
            if (protocol == null) {
                result.put("success", false);
                result.put("errorMessage", "标准协议不存在");
            } else {
                result.put("success", true);
                result.put("data", protocol);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存标准协议
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveProtocol(@RequestBody SaveProtocolRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            StandardProtocol protocol = protocolService.saveProtocol(
                request.getId(),
                request.getName(),
                request.getCode(),
                request.getDescription(),
                request.getProtocolType(),
                request.getDataFormat(),
                request.getCategory()
            );
            result.put("success", true);
            result.put("data", protocol);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除标准协议
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProtocol(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean deleted = protocolService.removeById(id);
            result.put("success", deleted);
            if (!deleted) {
                result.put("errorMessage", "删除失败，协议不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 切换启用状态
     */
    @PostMapping("/{id}/toggle-enabled")
    public ResponseEntity<Map<String, Object>> toggleEnabled(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = protocolService.toggleEnabled(id);
            result.put("success", success);
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存协议请求DTO
     */
    @Data
    public static class SaveProtocolRequest {
        private Long id;
        private String name;
        private String code;
        private String description;
        private String protocolType;
        private String dataFormat;
        private String category;
    }
}

