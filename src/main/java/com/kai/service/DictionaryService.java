package com.kai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kai.mapper.DictionaryItemMapper;
import com.kai.mapper.DictionaryMapper;
import com.kai.model.Dictionary;
import com.kai.model.DictionaryItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典服务类
 */
@Service
@Slf4j
public class DictionaryService extends ServiceImpl<DictionaryMapper, Dictionary> {
    
    @Autowired
    private DictionaryItemMapper dictionaryItemMapper;
    
    /**
     * 保存字典（包括字典项）
     */
    @Transactional
    public Dictionary saveDictionary(Long id, String name, String code, String description, List<DictionaryItemDTO> items) {
        Dictionary dict;
        if (id != null) {
            // 更新
            dict = this.getById(id);
            if (dict == null) {
                throw new IllegalArgumentException("字典不存在，ID: " + id);
            }
            dict.setName(name);
            dict.setCode(code);
            dict.setDescription(description);
            dict.setUpdateTime(LocalDateTime.now());
            this.updateById(dict);
            
            // 删除旧的字典项
            dictionaryItemMapper.delete(new LambdaQueryWrapper<DictionaryItem>()
                    .eq(DictionaryItem::getDictionaryId, id));
        } else {
            // 新增
            // 检查名称和编码是否已存在
            if (this.existsByName(name)) {
                throw new IllegalArgumentException("字典名称已存在: " + name);
            }
            if (this.existsByCode(code)) {
                throw new IllegalArgumentException("字典编码已存在: " + code);
            }
            
            dict = new Dictionary();
            dict.setName(name);
            dict.setCode(code);
            dict.setDescription(description);
            dict.setCreateTime(LocalDateTime.now());
            dict.setUpdateTime(LocalDateTime.now());
            this.save(dict);
        }
        
        // 保存字典项
        if (items != null && !items.isEmpty()) {
            int sortOrder = 0;
            for (DictionaryItemDTO item : items) {
                DictionaryItem dictItem = new DictionaryItem();
                dictItem.setDictionaryId(dict.getId());
                dictItem.setDictKey(item.getKey());
                dictItem.setDictValue(item.getValue());
                dictItem.setSortOrder(sortOrder++);
                dictItem.setCreateTime(LocalDateTime.now());
                dictItem.setUpdateTime(LocalDateTime.now());
                dictionaryItemMapper.insert(dictItem);
            }
        }
        
        return dict;
    }
    
    /**
     * 获取字典及其项
     */
    public DictionaryWithItems getDictionaryWithItems(Long id) {
        Dictionary dict = this.getById(id);
        if (dict == null) {
            return null;
        }
        
        List<DictionaryItem> items = dictionaryItemMapper.selectList(
                new LambdaQueryWrapper<DictionaryItem>()
                        .eq(DictionaryItem::getDictionaryId, id)
                        .orderByAsc(DictionaryItem::getSortOrder)
        );
        
        DictionaryWithItems result = new DictionaryWithItems();
        result.setDictionary(dict);
        result.setItems(items.stream()
                .map(item -> new DictionaryItemDTO(item.getDictKey(), item.getDictValue()))
                .collect(Collectors.toList()));
        
        return result;
    }
    
    /**
     * 获取字典的键值对Map（用于转换）
     * @param dictionaryId 字典ID
     * @param reverse 是否反向（true: v->k, false: k->v）
     * @return 键值对Map
     */
    public Map<String, String> getDictionaryMap(Long dictionaryId, boolean reverse) {
        List<DictionaryItem> items = dictionaryItemMapper.selectList(
                new LambdaQueryWrapper<DictionaryItem>()
                        .eq(DictionaryItem::getDictionaryId, dictionaryId)
                        .orderByAsc(DictionaryItem::getSortOrder)
        );
        
        Map<String, String> map = new HashMap<>();
        for (DictionaryItem item : items) {
            if (reverse) {
                // 反向：value -> key
                map.put(item.getDictValue(), item.getDictKey());
            } else {
                // 正向：key -> value
                map.put(item.getDictKey(), item.getDictValue());
            }
        }
        return map;
    }
    
    /**
     * 检查名称是否存在
     */
    public boolean existsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getName, name.trim())) > 0;
    }
    
    /**
     * 检查编码是否存在（排除指定ID）
     */
    public boolean existsByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getCode, code.trim())) > 0;
    }
    
    /**
     * 检查编码是否存在（排除指定ID）
     */
    public boolean existsByCodeExcludingId(String code, Long excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<Dictionary> wrapper = new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(Dictionary::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }
    
    /**
     * 删除字典（包括字典项）
     */
    @Transactional
    public boolean deleteDictionary(Long id) {
        // 删除字典项
        dictionaryItemMapper.delete(new LambdaQueryWrapper<DictionaryItem>()
                .eq(DictionaryItem::getDictionaryId, id));
        
        // 删除字典
        return this.removeById(id);
    }
    
    /**
     * 字典项DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DictionaryItemDTO {
        private String key;
        private String value;
    }
    
    /**
     * 字典及字典项
     */
    @lombok.Data
    public static class DictionaryWithItems {
        private Dictionary dictionary;
        private List<DictionaryItemDTO> items;
    }
}







