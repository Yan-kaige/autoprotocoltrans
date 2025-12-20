package com.kai.controller;

import com.kai.dto.TransformResponse;
import com.kai.model.MappingConfig;
import com.kai.service.TransformationEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 请求DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransformRequestV2 {
        private String sourceData;
        private MappingConfig mappingConfig;
    }
}

