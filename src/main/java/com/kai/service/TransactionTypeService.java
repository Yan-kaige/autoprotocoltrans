package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kai.mapper.TransactionTypeMapper;
import com.kai.model.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易类型服务类
 */
@Service
@Slf4j
public class TransactionTypeService extends ServiceImpl<TransactionTypeMapper, TransactionType> {
    
    /**
     * 保存交易类型（新增或更新）
     */
    @Transactional
    public TransactionType saveTransactionType(Long id, Long bankId, String transactionName, String description, Boolean enabled) {
        TransactionType transactionType;
        if (id != null) {
            // 更新
            transactionType = this.getById(id);
            if (transactionType == null) {
                throw new IllegalArgumentException("交易类型不存在，ID: " + id);
            }
            
            // 如果交易名称改变了，检查新名称是否已被该银行的其他交易类型使用
            if (!transactionName.equals(transactionType.getTransactionName())) {
                LambdaQueryWrapper<TransactionType> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(TransactionType::getBankId, bankId);
                wrapper.eq(TransactionType::getTransactionName, transactionName);
                TransactionType existing = this.getOne(wrapper);
                if (existing != null && !existing.getId().equals(id)) {
                    throw new IllegalArgumentException("该银行已存在同名交易类型: " + transactionName);
                }
            }
            
            transactionType.setBankId(bankId);
            transactionType.setTransactionName(transactionName);
            transactionType.setDescription(description);
            transactionType.setEnabled(enabled != null ? enabled : true);
            transactionType.setUpdateTime(LocalDateTime.now());
            this.updateById(transactionType);
        } else {
            // 新增
            // 检查该银行是否已存在同名交易类型
            LambdaQueryWrapper<TransactionType> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TransactionType::getBankId, bankId);
            wrapper.eq(TransactionType::getTransactionName, transactionName);
            TransactionType existing = this.getOne(wrapper);
            if (existing != null) {
                throw new IllegalArgumentException("该银行已存在同名交易类型: " + transactionName);
            }
            
            transactionType = new TransactionType();
            transactionType.setBankId(bankId);
            transactionType.setTransactionName(transactionName);
            transactionType.setDescription(description);
            transactionType.setEnabled(enabled != null ? enabled : true);
            transactionType.setCreateTime(LocalDateTime.now());
            transactionType.setUpdateTime(LocalDateTime.now());
            this.save(transactionType);
        }
        
        return transactionType;
    }
    
    /**
     * 根据银行ID获取所有交易类型
     * 如果预置的交易类型不存在，则自动创建
     */
    @Transactional
    public List<TransactionType> getByBankId(Long bankId) {
        // 获取数据库中已存在的交易类型
        LambdaQueryWrapper<TransactionType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionType::getBankId, bankId);
        List<TransactionType> existingTypes = this.list(wrapper);
        
        // 将已存在的交易类型按名称建立映射
        Map<String, TransactionType> existingMap = existingTypes.stream()
                .collect(Collectors.toMap(TransactionType::getTransactionName, type -> type));
        
        // 获取所有预置的交易类型
        com.kai.enums.TransactionType[] presetTypes = com.kai.enums.TransactionType.values();
        List<TransactionType> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 确保所有预置类型都存在
        for (com.kai.enums.TransactionType presetType : presetTypes) {
            String displayName = presetType.getDisplayName();
            TransactionType typeModel = existingMap.get(displayName);
            
            if (typeModel == null) {
                // 如果不存在，自动创建
                typeModel = new TransactionType();
                typeModel.setBankId(bankId);
                typeModel.setTransactionName(displayName);
                typeModel.setDescription(null);
                typeModel.setEnabled(true);
                typeModel.setCreateTime(now);
                typeModel.setUpdateTime(now);
                this.save(typeModel);
                log.info("自动创建预置交易类型: bankId={}, transactionName={}", bankId, displayName);
            }
            
            result.add(typeModel);
        }
        
        // 按交易名称排序
        result.sort((a, b) -> a.getTransactionName().compareTo(b.getTransactionName()));
        
        return result;
    }
    
    /**
     * 根据银行ID和交易名称获取交易类型
     */
    public TransactionType getByBankIdAndName(Long bankId, String transactionName) {
        LambdaQueryWrapper<TransactionType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionType::getBankId, bankId);
        wrapper.eq(TransactionType::getTransactionName, transactionName);
        return this.getOne(wrapper);
    }
    
    /**
     * 删除交易类型
     */
    @Transactional
    public boolean deleteTransactionType(Long id) {
        TransactionType transactionType = this.getById(id);
        if (transactionType == null) {
            throw new IllegalArgumentException("交易类型不存在，ID: " + id);
        }
        return this.removeById(id);
    }
}


