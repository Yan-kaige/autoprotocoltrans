package com.kai.controller;

import com.kai.service.TransformFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 函数控制器
 */
@RestController
@RequestMapping("/api/functions")
@CrossOrigin(origins = "*")
public class FunctionController {
    
    @Autowired
    private TransformFunctionService functionService;
    
    /**
     * 获取所有可用函数
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAvailableFunctions() {
        return ResponseEntity.ok(functionService.getAvailableFunctions());
    }
}

