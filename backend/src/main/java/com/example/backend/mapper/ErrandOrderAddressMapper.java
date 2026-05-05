package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ErrandOrderAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单地址快照Mapper接口
 */
@Mapper
public interface ErrandOrderAddressMapper extends BaseMapper<ErrandOrderAddress> {
}
