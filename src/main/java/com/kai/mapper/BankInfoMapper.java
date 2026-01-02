package com.kai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kai.model.BankInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行信息Mapper
 */
@Mapper
public interface BankInfoMapper extends BaseMapper<BankInfo> {
}

