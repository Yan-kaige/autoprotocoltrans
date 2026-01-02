package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kai.mapper.MappingConfigV2Mapper;
import com.kai.model.MappingConfig;
import com.kai.model.MappingConfigV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 映射配置服务类（新结构）
 */
@Service
@Slf4j
public class MappingConfigV2Service extends ServiceImpl<MappingConfigV2Mapper, MappingConfigV2> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private TransactionTypeService transactionTypeService;
    
    /**
     * 保存配置（新增或更新）
     */
    @Transactional
    public MappingConfigV2 saveConfig(Long id, Long transactionTypeId, String configType, String version,
                                      String name, String description, MappingConfig config) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        
        name = name.trim();
        version = version != null && !version.trim().isEmpty() ? version.trim() : "v1";
        
        // 将MappingConfig转为JSON字符串
        String configContent = objectMapper.writeValueAsString(config);
        
        MappingConfigV2 entity;
        
        if (id != null) {
            // 更新模式
            entity = this.getById(id);
            if (entity == null) {
                throw new IllegalArgumentException("配置不存在，ID: " + id);
            }
            
            entity.setName(name);
            entity.setDescription(description);
            entity.setConfigContent(configContent);
            entity.setUpdateTime(LocalDateTime.now());
            this.updateById(entity);
        } else {
            // 新建模式
            entity = new MappingConfigV2();
            entity.setTransactionTypeId(transactionTypeId);
            entity.setConfigType(configType);
            entity.setVersion(version);
            entity.setName(name);
            entity.setDescription(description);
            entity.setConfigContent(configContent);
            entity.setEnabled(true);
            entity.setIsCurrent(false); // 新建时默认不是当前版本
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            this.save(entity);
            
            // 如果是第一个版本，设置为当前版本
            setCurrentVersionIfFirst(transactionTypeId, configType, version);
        }
        
        return entity;
    }
    
    /**
     * 如果是第一个版本，设置为当前版本
     */
    private void setCurrentVersionIfFirst(Long transactionTypeId, String configType, String version) {
        LambdaQueryWrapper<MappingConfigV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
        wrapper.eq(MappingConfigV2::getConfigType, configType);
        long count = this.count(wrapper);
        if (count == 1) {
            // 这是第一个版本，设置为当前版本
            LambdaUpdateWrapper<MappingConfigV2> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
            updateWrapper.eq(MappingConfigV2::getConfigType, configType);
            updateWrapper.eq(MappingConfigV2::getVersion, version);
            updateWrapper.set(MappingConfigV2::getIsCurrent, true);
            this.update(updateWrapper);
        }
    }
    
    /**
     * 获取当前版本的配置
     */
    public MappingConfigV2 getCurrentConfig(Long transactionTypeId, String configType) {
        LambdaQueryWrapper<MappingConfigV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
        wrapper.eq(MappingConfigV2::getConfigType, configType);
        wrapper.eq(MappingConfigV2::getIsCurrent, true);
        return this.getOne(wrapper);
    }
    
    /**
     * 获取指定版本的配置
     */
    public MappingConfigV2 getConfigByVersion(Long transactionTypeId, String configType, String version) {
        LambdaQueryWrapper<MappingConfigV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
        wrapper.eq(MappingConfigV2::getConfigType, configType);
        wrapper.eq(MappingConfigV2::getVersion, version);
        return this.getOne(wrapper);
    }
    
    /**
     * 获取所有版本列表
     */
    public List<String> getVersions(Long transactionTypeId) {
        LambdaQueryWrapper<MappingConfigV2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
        wrapper.select(MappingConfigV2::getVersion);
        wrapper.groupBy(MappingConfigV2::getVersion);
        wrapper.orderByDesc(MappingConfigV2::getVersion);
        
        List<MappingConfigV2> entities = this.list(wrapper);
        return entities.stream()
                .map(MappingConfigV2::getVersion)
                .filter(v -> v != null && !v.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取下一个版本号
     */
    public String getNextVersion(Long transactionTypeId) {
        List<String> versions = getVersions(transactionTypeId);
        if (versions.isEmpty()) {
            return "v1";
        }
        
        int maxVersion = 1;
        for (String version : versions) {
            try {
                if (version != null && version.startsWith("v")) {
                    int num = Integer.parseInt(version.substring(1));
                    if (num > maxVersion) {
                        maxVersion = num;
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        return "v" + (maxVersion + 1);
    }
    
    /**
     * 创建新版本（复制当前版本）
     */
    @Transactional
    public String createNewVersion(Long transactionTypeId, String configType, String newConfigContent) throws Exception {
        // 获取当前版本的配置
        MappingConfigV2 currentConfig = getCurrentConfig(transactionTypeId, configType);
        if (currentConfig == null) {
            throw new IllegalArgumentException("当前版本配置不存在");
        }
        
        // 获取下一个版本号
        String newVersion = getNextVersion(transactionTypeId);
        
        // 复制当前版本的请求和响应配置为新版本
        // 1. 保存当前配置类型的新版本（使用新的配置内容）
        MappingConfigV2 newConfig = new MappingConfigV2();
        newConfig.setTransactionTypeId(transactionTypeId);
        newConfig.setConfigType(configType);
        newConfig.setVersion(newVersion);
        newConfig.setName(currentConfig.getName().replace(currentConfig.getVersion(), newVersion));
        newConfig.setDescription(currentConfig.getDescription());
        newConfig.setConfigContent(newConfigContent); // 使用新的配置内容
        newConfig.setEnabled(true);
        newConfig.setIsCurrent(true); // 新版本设为当前版本
        newConfig.setCreateTime(LocalDateTime.now());
        newConfig.setUpdateTime(LocalDateTime.now());
        this.save(newConfig);
        
        // 2. 取消旧版本的当前标记
        LambdaUpdateWrapper<MappingConfigV2> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
        updateWrapper.eq(MappingConfigV2::getConfigType, configType);
        updateWrapper.eq(MappingConfigV2::getVersion, currentConfig.getVersion());
        updateWrapper.set(MappingConfigV2::getIsCurrent, false);
        this.update(updateWrapper);
        
        // 3. 如果存在另一个配置类型（请求/响应），也复制到新版本
        String otherConfigType = "REQUEST".equals(configType) ? "RESPONSE" : "REQUEST";
        MappingConfigV2 otherCurrentConfig = getCurrentConfig(transactionTypeId, otherConfigType);
        if (otherCurrentConfig != null) {
            MappingConfigV2 newOtherConfig = new MappingConfigV2();
            newOtherConfig.setTransactionTypeId(transactionTypeId);
            newOtherConfig.setConfigType(otherConfigType);
            newOtherConfig.setVersion(newVersion);
            newOtherConfig.setName(otherCurrentConfig.getName().replace(otherCurrentConfig.getVersion(), newVersion));
            newOtherConfig.setDescription(otherCurrentConfig.getDescription());
            newOtherConfig.setConfigContent(otherCurrentConfig.getConfigContent()); // 复制配置内容
            newOtherConfig.setEnabled(true);
            newOtherConfig.setIsCurrent(true); // 新版本设为当前版本
            newOtherConfig.setCreateTime(LocalDateTime.now());
            newOtherConfig.setUpdateTime(LocalDateTime.now());
            this.save(newOtherConfig);
            
            // 取消旧版本的当前标记
            LambdaUpdateWrapper<MappingConfigV2> updateOtherWrapper = new LambdaUpdateWrapper<>();
            updateOtherWrapper.eq(MappingConfigV2::getTransactionTypeId, transactionTypeId);
            updateOtherWrapper.eq(MappingConfigV2::getConfigType, otherConfigType);
            updateOtherWrapper.eq(MappingConfigV2::getVersion, otherCurrentConfig.getVersion());
            updateOtherWrapper.set(MappingConfigV2::getIsCurrent, false);
            this.update(updateOtherWrapper);
        }
        
        return newVersion;
    }
    
    /**
     * 回退到指定版本（更新当前版本的内容为目标版本的内容）
     */
    @Transactional
    public boolean rollbackToVersion(Long transactionTypeId, String targetVersion) {
        // 获取目标版本的配置
        MappingConfigV2 targetRequestConfig = getConfigByVersion(transactionTypeId, "REQUEST", targetVersion);
        MappingConfigV2 targetResponseConfig = getConfigByVersion(transactionTypeId, "RESPONSE", targetVersion);
        
        if (targetRequestConfig == null && targetResponseConfig == null) {
            throw new IllegalArgumentException("指定版本的配置不存在");
        }
        
        // 获取当前版本的配置
        MappingConfigV2 currentRequestConfig = getCurrentConfig(transactionTypeId, "REQUEST");
        MappingConfigV2 currentResponseConfig = getCurrentConfig(transactionTypeId, "RESPONSE");
        
        // 更新当前版本的配置内容为目标版本的内容
        if (currentRequestConfig != null && targetRequestConfig != null) {
            currentRequestConfig.setConfigContent(targetRequestConfig.getConfigContent());
            currentRequestConfig.setUpdateTime(LocalDateTime.now());
            this.updateById(currentRequestConfig);
        }
        
        if (currentResponseConfig != null && targetResponseConfig != null) {
            currentResponseConfig.setConfigContent(targetResponseConfig.getConfigContent());
            currentResponseConfig.setUpdateTime(LocalDateTime.now());
            this.updateById(currentResponseConfig);
        }
        
        return true;
    }
    
    /**
     * 根据ID获取配置
     */
    public MappingConfigV2 getConfigById(Long id) {
        MappingConfigV2 entity = this.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("配置不存在，ID: " + id);
        }
        return entity;
    }
    
    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfigById(Long id) {
        MappingConfigV2 entity = this.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("配置不存在，ID: " + id);
        }
        this.removeById(id);
    }
}


