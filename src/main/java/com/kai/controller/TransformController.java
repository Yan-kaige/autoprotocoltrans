package com.kai.controller;

import com.kai.dto.TransformResponse;
import com.kai.model.MappingConfig;
import com.kai.service.TransformationEngine;
import com.kai.util.MessageConverterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换控制器（新版本，使用MappingConfig）
 */
@RestController
@RequestMapping("/api/v2/transform")
@CrossOrigin(origins = "*")
public class TransformController {
    
    @Autowired
    private TransformationEngine transformationEngine;
    
    /**
     * 执行转换（使用MappingConfig配置）
     * 
     * @param request 包含sourceData和mappingConfig的请求
     * @return 转换结果
     */
    @PostMapping
    public ResponseEntity<TransformResponse> transform(@RequestBody TransformRequestV2 request) {
        TransformResponse response = new TransformResponse();
        try {
            String sourceData = request.getSourceData();
            MappingConfig config = request.getMappingConfig();
            
            if (sourceData == null || config == null) {
                response.setSuccess(false);
                response.setErrorMessage("源数据和配置不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            String result = transformationEngine.transform(sourceData, config);
            response.setSuccess(true);
            response.setTransformedData(result);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * 调试接口：查看XML/JSON解析后的结构
     */
    @PostMapping("/debug/parse")
    public ResponseEntity<Map<String, Object>> debugParse(@RequestBody DebugParseRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sourceData = request.getSourceData();
            String sourceType = request.getSourceType();
            
            if (sourceData == null || sourceType == null) {
                result.put("error", "源数据和类型不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 解析数据
            Map<String, Object> parsedMap = MessageConverterUtil.parseToMap(sourceData, sourceType);
            
            // 转换为JSON字符串以便查看
            String jsonString = MessageConverterUtil.mapToString(parsedMap, "JSON", true);
            
            result.put("success", true);
            result.put("parsedJson", jsonString);
            result.put("parsedMap", parsedMap);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 请求DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransformRequestV2 {
        private String sourceData;
        private MappingConfig mappingConfig;
    }
    
    /**
     * 调试解析请求DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DebugParseRequest {
        private String sourceData;
        private String sourceType; // "JSON" 或 "XML"
    }
}

