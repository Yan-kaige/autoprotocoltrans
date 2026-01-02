package com.kai.controller;

import com.kai.model.StandardProtocol;
import com.kai.service.LlmService;
import com.kai.service.StandardProtocolService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    
    @Autowired
    private LlmService llmService;
    
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
     * 从文档导入协议
     * 支持文本内容和文件上传两种方式
     */
    @PostMapping("/import-from-document")
    public ResponseEntity<Map<String, Object>> importFromDocument(
            @RequestParam(required = false) String documentContent,
            @RequestParam(required = false) MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String content = documentContent;
            
            // 如果上传了文件，读取文件内容
            if (file != null && !file.isEmpty()) {
                try {
                    content = new String(file.getBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    result.put("success", false);
                    result.put("errorMessage", "读取文件失败: " + e.getMessage());
                    return ResponseEntity.ok(result);
                }
            }
            
            if (content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("errorMessage", "文档内容不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 使用LLM分析文档（可能返回多条协议）
            List<Map<String, Object>> analysisResults = llmService.analyzeProtocolFromDocument(content);
            
            // 保存所有协议
            List<StandardProtocol> savedProtocols = new ArrayList<>();
            List<Map<String, Object>> savedResults = new ArrayList<>();
            
            for (Map<String, Object> analysisResult : analysisResults) {
                // 创建协议对象
                String name = (String) analysisResult.getOrDefault("name", "从文档导入的协议");
                String code = (String) analysisResult.getOrDefault("code", "imported_" + System.currentTimeMillis());
                String description = (String) analysisResult.getOrDefault("description", "从文档自动导入");
                String protocolType = (String) analysisResult.getOrDefault("protocolType", "JSON");
                String dataFormat = (String) analysisResult.getOrDefault("dataFormat", "{}");
                String category = (String) analysisResult.getOrDefault("category", "导入协议");
                
                // 检查编码是否已存在，如果存在则添加时间戳
                StandardProtocol existing = protocolService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardProtocol>()
                        .eq(StandardProtocol::getCode, code)
                );
                if (existing != null) {
                    code = code + "_" + System.currentTimeMillis();
                }
                
                // 保存协议
                StandardProtocol protocol = protocolService.saveProtocol(
                    null,
                    name,
                    code,
                    description,
                    protocolType,
                    dataFormat,
                    category
                );
                
                savedProtocols.add(protocol);
                savedResults.add(analysisResult);
            }
            
            result.put("success", true);
            result.put("data", savedProtocols.size() == 1 ? savedProtocols.get(0) : savedProtocols); // 单个协议返回对象，多个返回数组
            result.put("count", savedProtocols.size());
            result.put("analysisResults", savedResults);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", "导入失败: " + e.getMessage());
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

