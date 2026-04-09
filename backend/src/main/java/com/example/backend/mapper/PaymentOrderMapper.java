package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 支付流水Mapper接口
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 按支付状态汇总支付金额
     *
     * @param payStatus 支付状态
     * @return 金额汇总，无匹配时返回null
     */
    @Select("SELECT SUM(pay_amount) FROM payment_order WHERE pay_status = #{payStatus} AND is_deleted = 0")
    BigDecimal sumPayAmountByStatus(@Param("payStatus") Integer payStatus);
}
