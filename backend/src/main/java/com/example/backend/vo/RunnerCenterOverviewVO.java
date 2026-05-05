package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 跑腿员个人中心概览响应视图对象
 */
@Data
@Builder
public class RunnerCenterOverviewVO {

    /**
     * 个人资料
     */
    private UserProfileVO profile;

    /**
     * 认证信息
     */
    private RunnerAuthProfileVO authInfo;

    /**
     * 已接单数
     */
    private Long acceptedOrderCount;

    /**
     * 进行中订单数（已接单、已联系、已取件、配送中、已送达）
     */
    private Long ongoingOrderCount;

    /**
     * 已完成订单数
     */
    private Long completedOrderCount;

    /**
     * 总收益
     */
    private BigDecimal totalIncome;

    /**
     * 待结算收益
     */
    private BigDecimal pendingIncome;

    /**
     * 已结算收益
     */
    private BigDecimal settledIncome;

    /**
     * 平均评分
     */
    private BigDecimal averageScore;

    /**
     * 评价总数
     */
    private Long totalEvaluationCount;
}
