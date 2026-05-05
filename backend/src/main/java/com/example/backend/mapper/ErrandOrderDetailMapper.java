package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ErrandOrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跑腿订单分类扩展详情Mapper接口
 */
@Mapper
public interface ErrandOrderDetailMapper extends BaseMapper<ErrandOrderDetail> {
}
