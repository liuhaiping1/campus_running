package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水Mapper接口
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
