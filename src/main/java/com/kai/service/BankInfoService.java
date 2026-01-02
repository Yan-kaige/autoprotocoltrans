package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kai.mapper.BankInfoMapper;
import com.kai.model.BankInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 银行信息服务类
 */
@Service
@Slf4j
public class BankInfoService extends ServiceImpl<BankInfoMapper, BankInfo> {
    
    /**
     * 保存银行信息（新增或更新）
     */
    @Transactional
    public BankInfo saveBankInfo(Long id, String name, String code, String description, Boolean enabled) {
        BankInfo bankInfo;
        if (id != null) {
            // 更新
            bankInfo = this.getById(id);
            if (bankInfo == null) {
                throw new IllegalArgumentException("银行信息不存在，ID: " + id);
            }
            
            // 如果名称改变了，检查新名称是否已被其他银行使用
            if (!name.equals(bankInfo.getName())) {
                if (this.existsByName(name)) {
                    throw new IllegalArgumentException("银行名称已存在: " + name);
                }
            }
            
            // 如果编码改变了，检查新编码是否已被其他银行使用
            if (!code.equals(bankInfo.getCode())) {
                if (this.existsByCode(code)) {
                    throw new IllegalArgumentException("银行编码已存在: " + code);
                }
            }
            
            bankInfo.setName(name);
            bankInfo.setCode(code);
            bankInfo.setDescription(description);
            bankInfo.setEnabled(enabled != null ? enabled : true);
            bankInfo.setUpdateTime(LocalDateTime.now());
            this.updateById(bankInfo);
        } else {
            // 新增
            // 检查名称和编码是否已存在
            if (this.existsByName(name)) {
                throw new IllegalArgumentException("银行名称已存在: " + name);
            }
            if (this.existsByCode(code)) {
                throw new IllegalArgumentException("银行编码已存在: " + code);
            }
            
            bankInfo = new BankInfo();
            bankInfo.setName(name);
            bankInfo.setCode(code);
            bankInfo.setDescription(description);
            bankInfo.setEnabled(enabled != null ? enabled : true);
            bankInfo.setCreateTime(LocalDateTime.now());
            bankInfo.setUpdateTime(LocalDateTime.now());
            this.save(bankInfo);
        }
        
        return bankInfo;
    }
    
    /**
     * 删除银行信息
     */
    @Transactional
    public boolean deleteBankInfo(Long id) {
        BankInfo bankInfo = this.getById(id);
        if (bankInfo == null) {
            throw new IllegalArgumentException("银行信息不存在，ID: " + id);
        }
        return this.removeById(id);
    }
    
    /**
     * 检查名称是否存在
     */
    public boolean existsByName(String name) {
        LambdaQueryWrapper<BankInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankInfo::getName, name);
        return this.exists(wrapper);
    }
    
    /**
     * 检查编码是否存在
     */
    public boolean existsByCode(String code) {
        LambdaQueryWrapper<BankInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankInfo::getCode, code);
        return this.exists(wrapper);
    }
    
    /**
     * 检查编码是否存在（排除指定ID）
     */
    public boolean existsByCodeExcludingId(String code, Long excludeId) {
        LambdaQueryWrapper<BankInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankInfo::getCode, code);
        wrapper.ne(BankInfo::getId, excludeId);
        return this.exists(wrapper);
    }
    
    /**
     * 获取所有启用的银行信息列表
     */
    public List<BankInfo> getEnabledBanks() {
        LambdaQueryWrapper<BankInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankInfo::getEnabled, true);
        wrapper.orderByAsc(BankInfo::getName);
        return this.list(wrapper);
    }
}


