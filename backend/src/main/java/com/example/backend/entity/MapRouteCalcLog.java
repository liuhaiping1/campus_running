package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 地图路线计算日志实体类
 * <p>
 * 记录每次调用地图服务计算路线的输入参数和返回结果，
 * 用于费用计算追溯和地图服务调用审计。
 * </p>
 */
@Data
@TableName("map_route_calc_log")
public class MapRouteCalcLog implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 本次计算请求ID（幂等）
     */
    private String requestId;

    /**
     * 地图服务商：1高德 2百度 3腾讯 9系统兜底
     */
    private Integer mapProvider;

    /**
     * 路线策略：walking/bicycling/driving
     */
    private String routeStrategy;

    /**
     * 起点经度
     */
    private BigDecimal originLng;

    /**
     * 起点纬度
     */
    private BigDecimal originLat;

    /**
     * 终点经度
     */
    private BigDecimal destinationLng;

    /**
     * 终点纬度
     */
    private BigDecimal destinationLat;

    /**
     * 路线距离，单位米
     */
    private Integer routeDistanceM;

    /**
     * 直线距离，单位米
     */
    private Integer straightDistanceM;

    /**
     * 预计耗时，单位秒
     */
    private Integer durationSec;

    /**
     * 计算状态：0失败 1成功 2兜底成功
     */
    private Integer calcStatus;

    /**
     * 地图服务错误码
     */
    private String errorCode;

    /**
     * 地图服务错误信息
     */
    private String errorMsg;

    /**
     * 地图服务返回摘要，不保存完整敏感信息
     */
    private String responseSummary;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer isDeleted;
}
