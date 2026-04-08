package com.example.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 跑腿员收益总览 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerIncomeOverviewVO {

    /**
     * 累计收益。
     */
    private BigDecimal totalIncome;

    /**
     * 待结算收益。
     */
    private BigDecimal pendingIncome;

    /**
     * 已结算收益。
     */
    private BigDecimal settledIncome;

    /**
     * 结算中收益。
     */
    private BigDecimal settlingIncome;

    /**
     * 已回滚收益。
     */
    private BigDecimal rolledBackIncome;

    /**
     * 本月收益。
     */
    private BigDecimal currentMonthIncome;

    /**
     * 收益订单数量。
     */
    private Long totalOrderCount;
}
