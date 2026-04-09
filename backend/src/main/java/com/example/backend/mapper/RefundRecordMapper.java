package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 退款记录Mapper接口
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    /**
     * 按退款状态汇总退款金额
     *
     * @param refundStatus 退款状态
     * @return 金额汇总，无匹配时返回null
     */
    @Select("SELECT SUM(refund_amount) FROM refund_record WHERE refund_status = #{refundStatus} AND is_deleted = 0")
    BigDecimal sumRefundAmountByStatus(@Param("refundStatus") Integer refundStatus);
}
