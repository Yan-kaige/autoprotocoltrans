package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kai.mapper.MappingConfigMapper;
import com.kai.model.MappingConfig;
import com.kai.model.MappingConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 映射配置服务类
 */
@Service
@Slf4j
public class MappingConfigService extends ServiceImpl<MappingConfigMapper, MappingConfigEntity> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 保存配置
     */
    @Transactional
    public MappingConfigEntity saveConfig(String name, String description, MappingConfig config) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        
        name = name.trim();
        
        // 将MappingConfig转为JSON字符串
        String configContent = objectMapper.writeValueAsString(config);
        
        // 检查是否已存在同名配置
        LambdaQueryWrapper<MappingConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigEntity::getName, name);
        MappingConfigEntity existingEntity = this.getOne(wrapper);
        
        MappingConfigEntity entity;
        if (existingEntity != null) {
            // 更新现有配置
            entity = existingEntity;
            entity.setDescription(description);
            entity.setConfigContent(configContent);
            entity.setUpdateTime(LocalDateTime.now());
            this.updateById(entity);
        } else {
            // 创建新配置
            entity = new MappingConfigEntity();
            entity.setName(name);
            entity.setDescription(description);
            entity.setConfigContent(configContent);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            this.save(entity);
        }
        
        return entity;
    }
    
    /**
     * 根据名称获取配置
     */
    public MappingConfig getConfigByName(String name) throws Exception {
        LambdaQueryWrapper<MappingConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigEntity::getName, name);
        MappingConfigEntity entity = this.getOne(wrapper);
        
        if (entity == null) {
            throw new IllegalArgumentException("配置不存在: " + name);
        }
        
        return objectMapper.readValue(entity.getConfigContent(), MappingConfig.class);
    }
    
    /**
     * 获取所有配置列表（按更新时间倒序）
     */
    public List<MappingConfigEntity> getAllConfigs() {
        QueryWrapper<MappingConfigEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("update_time");
        return this.list(wrapper);
    }
    
    /**
     * 根据ID获取配置
     */
    public MappingConfigEntity getConfigById(Long id) {
        MappingConfigEntity entity = this.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("配置不存在，ID: " + id);
        }
        return entity;
    }
    
    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(String name) {
        LambdaQueryWrapper<MappingConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigEntity::getName, name);
        if (!this.exists(wrapper)) {
            throw new IllegalArgumentException("配置不存在: " + name);
        }
        this.remove(wrapper);
    }
    
    /**
     * 根据ID删除配置
     */
    @Transactional
    public void deleteConfigById(Long id) {
        MappingConfigEntity entity = this.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("配置不存在，ID: " + id);
        }
        this.removeById(id);
    }
    
    /**
     * 检查配置是否存在
     */
    public boolean existsByName(String name) {
        LambdaQueryWrapper<MappingConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigEntity::getName, name);
        return this.exists(wrapper);
    }
}

