package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建请求DTO
 */
@Data
public class OrderCreateRequest {

    /** 任务分类ID */
    @NotNull(message = "任务分类不能为空")
    private Long categoryId;

    /** 任务标题 */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题最长100字")
    private String title;

    /** 任务描述 */
    @NotBlank(message = "任务描述不能为空")
    @Size(max = 500, message = "任务描述最长500字")
    private String orderDesc;

    /** 取件地址 */
    @NotBlank(message = "取件地址不能为空")
    private String pickupAddress;

    /** 送达地址 */
    @NotBlank(message = "送达地址不能为空")
    private String deliveryAddress;

    /** 取件点经度 */
    private BigDecimal pickupLng;

    /** 取件点纬度 */
    private BigDecimal pickupLat;

    /** 送达点经度 */
    private BigDecimal deliveryLng;

    /** 送达点纬度 */
    private BigDecimal deliveryLat;

    /** 预估距离（公里） */
    @NotNull(message = "预估距离不能为空")
    @DecimalMin(value = "0", message = "距离不能为负数")
    private BigDecimal distanceKm;

    /** 小费 */
    @DecimalMin(value = "0", message = "小费不能为负数")
    private BigDecimal tipFee;

    /** 期望完成时间 */
    @NotNull(message = "期望完成时间不能为空")
    @Future(message = "期望完成时间必须晚于当前时间")
    private LocalDateTime deadlineTime;
}
