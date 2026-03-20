package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.OrderEvaluation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单评价Mapper接口
 */
@Mapper
public interface OrderEvaluationMapper extends BaseMapper<OrderEvaluation> {
}
