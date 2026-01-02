package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kai.mapper.CustomFunctionMapper;
import com.kai.model.CustomFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义函数服务类
 */
@Service
@Slf4j
public class CustomFunctionService extends ServiceImpl<CustomFunctionMapper, CustomFunction> {
    
    /**
     * 保存函数（新增或更新）
     */
    @Transactional
    public CustomFunction saveFunction(Long id, String name, String code, String description, String script, Boolean enabled) {
        CustomFunction func;
        if (id != null) {
            // 更新
            func = this.getById(id);
            if (func == null) {
                throw new IllegalArgumentException("函数不存在，ID: " + id);
            }
            func.setName(name);
            func.setCode(code);
            func.setDescription(description);
            func.setScript(script);
            func.setEnabled(enabled);
            func.setUpdateTime(LocalDateTime.now());
            this.updateById(func);
        } else {
            // 新增
            // 检查名称和编码是否已存在
            if (this.existsByName(name)) {
                throw new IllegalArgumentException("函数名称已存在: " + name);
            }
            if (this.existsByCode(code)) {
                throw new IllegalArgumentException("函数编码已存在: " + code);
            }
            
            func = new CustomFunction();
            func.setName(name);
            func.setCode(code);
            func.setDescription(description);
            func.setScript(script);
            func.setEnabled(enabled != null ? enabled : true);
            func.setCreateTime(LocalDateTime.now());
            func.setUpdateTime(LocalDateTime.now());
            this.save(func);
        }
        
        return func;
    }
    
    /**
     * 获取所有启用的函数
     */
    public List<CustomFunction> getEnabledFunctions() {
        return this.list(new LambdaQueryWrapper<CustomFunction>()
                .eq(CustomFunction::getEnabled, true)
                .orderByAsc(CustomFunction::getName));
    }
    
    /**
     * 根据编码获取函数
     */
    public CustomFunction getByCode(String code) {
        return this.getOne(new LambdaQueryWrapper<CustomFunction>()
                .eq(CustomFunction::getCode, code)
                .eq(CustomFunction::getEnabled, true), false);
    }
    
    /**
     * 检查名称是否存在
     */
    public boolean existsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<CustomFunction>()
                .eq(CustomFunction::getName, name.trim())) > 0;
    }
    
    /**
     * 检查编码是否存在
     */
    public boolean existsByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<CustomFunction>()
                .eq(CustomFunction::getCode, code.trim())) > 0;
    }
    
    /**
     * 检查编码是否存在（排除指定ID）
     */
    public boolean existsByCodeExcludingId(String code, Long excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<CustomFunction> wrapper = new LambdaQueryWrapper<CustomFunction>()
                .eq(CustomFunction::getCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(CustomFunction::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }
}









