package com.example.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿员收益明细 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerIncomeRecordVO {

    /**
     * 收益记录ID。
     */
    private Long id;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 跑腿员用户ID。
     */
    private Long runnerId;

    /**
     * 收益金额。
     */
    private BigDecimal incomeAmount;

    /**
     * 平台抽佣金额。
     */
    private BigDecimal commissionAmount;

    /**
     * 结算状态。
     */
    private Integer settlementStatus;

    /**
     * 结算时间。
     */
    private LocalDateTime settlementTime;

    /**
     * 回滚金额。
     */
    private BigDecimal rollbackAmount;

    /**
     * 回滚原因。
     */
    private String rollbackReason;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
