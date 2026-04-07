package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员后台统计概览视图对象
 * <p>
 * 封装管理首页所需的各项统计数据，包括订单统计、金额汇总、用户统计和待处理事项数。
 * 所有数值字段均有默认值，不会返回null。
 * </p>
 */
@Data
@Builder
public class AdminOverviewVO {

    /**
     * 订单总数（未逻辑删除的订单总数）
     */
    private Long totalOrderCount;

    /**
     * 今日新增订单数（当天创建的未逻辑删除订单数）
     */
    private Long todayOrderCount;

    /**
     * 已支付金额汇总（支付成功的流水金额总和）
     */
    private BigDecimal paidAmount;

    /**
     * 退款成功金额汇总（退款成功的退款金额总和）
     */
    private BigDecimal refundAmount;

    /**
     * 有效跑腿员数量（角色状态为有效的RUNNER用户数）
     */
    private Long activeRunnerCount;

    /**
     * 待审核跑腿员认证申请数
     */
    private Long pendingRunnerAuthCount;

    /**
     * 待处理申诉数（状态为待处理或处理中的申诉数）
     */
    private Long pendingAppealCount;

    /**
     * 待处理退款数（状态为待处理或处理中的退款数）
     */
    private Long pendingRefundCount;
}
