package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kai.mapper.TransactionTypeMapper;
import com.kai.model.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
     */
    public List<TransactionType> getByBankId(Long bankId) {
        LambdaQueryWrapper<TransactionType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionType::getBankId, bankId);
        wrapper.orderByAsc(TransactionType::getTransactionName);
        return this.list(wrapper);
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


