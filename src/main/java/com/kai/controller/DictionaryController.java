package com.kai.controller;

import com.kai.service.DictionaryService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典管理控制器
 */
@RestController
@RequestMapping("/api/v2/dictionary")
@Slf4j
public class DictionaryController {
    
    @Autowired
    private DictionaryService dictionaryService;
    
    /**
     * 获取所有字典列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllDictionaries() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<com.kai.model.Dictionary> dictionaries = dictionaryService.list();
            result.put("success", true);
            result.put("data", dictionaries);
        } catch (Exception e) {
            log.error("获取字典列表失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据ID获取字典及其项
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDictionary(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DictionaryService.DictionaryWithItems dictWithItems = dictionaryService.getDictionaryWithItems(id);
            if (dictWithItems == null) {
                result.put("success", false);
                result.put("errorMessage", "字典不存在");
            } else {
                result.put("success", true);
                result.put("data", dictWithItems);
            }
        } catch (Exception e) {
            log.error("获取字典失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 保存字典（新增或更新）
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveDictionary(@RequestBody SaveDictionaryRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            com.kai.model.Dictionary dictionary = dictionaryService.saveDictionary(
                    request.getId(),
                    request.getName(),
                    request.getCode(),
                    request.getDescription(),
                    request.getItems()
            );
            result.put("success", true);
            result.put("data", dictionary);
            result.put("message", request.getId() != null ? "字典更新成功" : "字典保存成功");
        } catch (Exception e) {
            log.error("保存字典失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除字典
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDictionary(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = dictionaryService.deleteDictionary(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除字典失败", e);
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
                    ? dictionaryService.existsByCodeExcludingId(code, excludeId)
                    : dictionaryService.existsByCode(code);
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
     * 保存字典请求DTO
     */
    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveDictionaryRequest {
        private Long id;
        private String name;
        private String code;
        private String description;
        private List<DictionaryService.DictionaryItemDTO> items;
    }
}







