package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ErrandOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跑腿订单Mapper接口
 */
@Mapper
public interface ErrandOrderMapper extends BaseMapper<ErrandOrder> {
}
