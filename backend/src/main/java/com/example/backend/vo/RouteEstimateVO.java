package com.example.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 路线预估返回视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEstimateVO {

    /** 直线距离，单位 km */
    private BigDecimal straightDistanceKm;

    /** 路线距离，单位 km，高德失败时为 null */
    private BigDecimal routeDistanceKm;

    /** 预计耗时，单位秒，高德失败时为 null */
    private Integer routeDurationSec;

    /** 距离来源：1 地图路线 2 直线兜底 */
    private Integer distanceSource;

    /** 距离计算状态：1 成功 2 失败兜底 */
    private Integer distanceCalcStatus;

    /** 地图服务商：1 高德 9 系统兜底 */
    private Integer mapProvider;

    /** 路线策略 */
    private String routeStrategy;

    /** 基础费用 */
    private BigDecimal baseFee;

    /** 距离费用 */
    private BigDecimal distanceFee;

    /** 重量费用 */
    private BigDecimal weightFee;

    /** 时段费用 */
    private BigDecimal timeFee;

    /** 加急附加费 */
    private BigDecimal urgentFee;

    /** 小费 */
    private BigDecimal tipFee;

    /** 订单总金额 */
    private BigDecimal orderAmount;

    /** 平台抽成 */
    private BigDecimal platformCommission;

    /** 预估跑腿员收益 */
    private BigDecimal estimatedRunnerIncome;

    /** 费用规则版本 */
    private String feeRuleVersion;

    /** 费用明细 JSON */
    private String feeDetail;

    /** 请求唯一标识 */
    private String requestId;

    /** 提示信息，如兜底提示 */
    private String message;
}
