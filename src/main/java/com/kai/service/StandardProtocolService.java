package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kai.mapper.StandardProtocolMapper;
import com.kai.model.StandardProtocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标准协议服务类
 */
@Service
@Slf4j
public class StandardProtocolService extends ServiceImpl<StandardProtocolMapper, StandardProtocol> {
    
    /**
     * 保存标准协议
     */
    @Transactional
    public StandardProtocol saveProtocol(Long id, String name, String code, String description, 
                                        String protocolType, String dataFormat, String category) {
        StandardProtocol protocol;
        if (id != null) {
            // 更新
            protocol = this.getById(id);
            if (protocol == null) {
                throw new IllegalArgumentException("标准协议不存在，ID: " + id);
            }
            protocol.setName(name);
            protocol.setCode(code);
            protocol.setDescription(description);
            protocol.setProtocolType(protocolType);
            protocol.setDataFormat(dataFormat);
            protocol.setCategory(category);
            protocol.setUpdateTime(LocalDateTime.now());
            this.updateById(protocol);
        } else {
            // 新增
            protocol = new StandardProtocol();
            protocol.setName(name);
            protocol.setCode(code);
            protocol.setDescription(description);
            protocol.setProtocolType(protocolType);
            protocol.setDataFormat(dataFormat);
            protocol.setCategory(category);
            protocol.setEnabled(true);
            protocol.setCreateTime(LocalDateTime.now());
            protocol.setUpdateTime(LocalDateTime.now());
            this.save(protocol);
        }
        return protocol;
    }
    
    /**
     * 根据协议类型获取启用的协议列表
     */
    public List<StandardProtocol> getEnabledProtocolsByType(String protocolType) {
        return this.list(new LambdaQueryWrapper<StandardProtocol>()
                .eq(StandardProtocol::getProtocolType, protocolType)
                .eq(StandardProtocol::getEnabled, true)
                .orderByAsc(StandardProtocol::getCategory)
                .orderByAsc(StandardProtocol::getName));
    }
    
    /**
     * 获取所有启用的协议列表
     */
    public List<StandardProtocol> getAllEnabledProtocols() {
        return this.list(new LambdaQueryWrapper<StandardProtocol>()
                .eq(StandardProtocol::getEnabled, true)
                .orderByAsc(StandardProtocol::getCategory)
                .orderByAsc(StandardProtocol::getName));
    }
    
    /**
     * 切换启用状态
     */
    @Transactional
    public boolean toggleEnabled(Long id) {
        StandardProtocol protocol = this.getById(id);
        if (protocol == null) {
            throw new IllegalArgumentException("标准协议不存在，ID: " + id);
        }
        protocol.setEnabled(!protocol.getEnabled());
        protocol.setUpdateTime(LocalDateTime.now());
        return this.updateById(protocol);
    }
}


