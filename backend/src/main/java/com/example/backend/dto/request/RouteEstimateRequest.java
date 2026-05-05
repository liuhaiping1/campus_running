package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 路线预估请求DTO
 */
@Data
public class RouteEstimateRequest {

    /** 任务分类ID */
    @NotNull(message = "任务分类不能为空")
    private Long categoryId;

    /** 起点经度 */
    @NotNull(message = "起点经度不能为空")
    private BigDecimal originLng;

    /** 起点纬度 */
    @NotNull(message = "起点纬度不能为空")
    private BigDecimal originLat;

    /** 终点经度 */
    @NotNull(message = "终点经度不能为空")
    private BigDecimal destinationLng;

    /** 终点纬度 */
    @NotNull(message = "终点纬度不能为空")
    private BigDecimal destinationLat;

    /** 路线策略：walking/bicycling，默认 bicycling */
    private String routeStrategy = "bicycling";

    /** 小费，默认 0 */
    @DecimalMin(value = "0", message = "小费不能为负数")
    private BigDecimal tipFee = BigDecimal.ZERO;
}
