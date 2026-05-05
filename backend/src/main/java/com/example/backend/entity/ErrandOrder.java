package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿订单实体类
 */
@Data
@TableName("errand_order")
public class ErrandOrder implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 接单跑腿员ID
     */
    private Long runnerId;

    /**
     * 任务分类ID
     */
    private Long categoryId;

    /**
     * 下单时使用的计费规则版本快照
     */
    private String feeRuleVersion;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String orderDesc;

    /**
     * 订单附件地址，多个用逗号或JSON存储
     */
    private String attachmentUrls;

    /**
     * 取件地址
     */
    private String pickupAddress;

    /**
     * 送达地址
     */
    private String deliveryAddress;

    /**
     * 订单联系人姓名，按下单时快照保存
     */
    private String contactName;

    /**
     * 订单联系人手机号，按下单时快照保存
     */
    private String contactPhone;

    /**
     * 取件点经度
     */
    private BigDecimal pickupLng;

    /**
     * 取件点纬度
     */
    private BigDecimal pickupLat;

    /**
     * 送达点经度
     */
    private BigDecimal deliveryLng;

    /**
     * 送达点纬度
     */
    private BigDecimal deliveryLat;

    /**
     * 预估距离
     */
    private BigDecimal distanceKm;

    /**
     * 地图路线距离，单位公里
     */
    private BigDecimal routeDistanceKm;

    /**
     * 路线预计耗时，单位秒
     */
    private Integer routeDurationSec;

    /**
     * 直线距离兜底值，单位公里
     */
    private BigDecimal straightDistanceKm;

    /**
     * 计费距离来源：1地图路线 2直线兜底 3管理员修正
     */
    private Integer distanceSource;

    /**
     * 距离计算状态：0未计算 1计算成功 2计算失败 3人工修正
     */
    private Integer distanceCalcStatus;

    /**
     * 地图服务商：1高德 2百度 3腾讯 9系统兜底
     */
    private Integer mapProvider;

    /**
     * 路线策略：walking/bicycling/driving
     */
    private String routeStrategy;

    /**
     * 距离计算时间
     */
    private LocalDateTime distanceCalcTime;

    /**
     * 基础费用
     */
    private BigDecimal baseFee;

    /**
     * 距离费用
     */
    private BigDecimal distanceFee;

    /**
     * 重量费用
     */
    private BigDecimal weightFee;

    /**
     * 时段费用
     */
    private BigDecimal timeFee;

    /**
     * 小费
     */
    private BigDecimal tipFee;

    /**
     * 订单总金额
     */
    private BigDecimal orderAmount;

    /**
     * 平台抽成
     */
    private BigDecimal platformCommission;

    /**
     * 预估跑腿员收益
     */
    private BigDecimal estimatedRunnerIncome;

    /**
     * 下单时费用计算明细快照（JSON字符串）
     */
    private String feeDetail;

    /**
     * 订单状态：0待支付 1待接单 2已接单 3已联系用户 4已取件 5配送中 6已送达 7已完成 8已取消 9已关闭 10申诉中
     */
    private Integer orderStatus;

    /**
     * 支付状态：0未支付 1支付中 2支付成功 3退款中 4已退款 5支付关闭 6部分退款
     */
    private Integer payStatus;

    /**
     * 结算状态：0待结算 1结算中 2已结算 3已回滚
     */
    private Integer settlementStatus;

    /**
     * 期望完成时间
     */
    private LocalDateTime deadlineTime;

    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;

    /**
     * 联系用户时间
     */
    private LocalDateTime contactTime;

    /**
     * 取件时间
     */
    private LocalDateTime pickupTime;

    /**
     * 送达时间
     */
    private LocalDateTime deliverTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 取消操作人ID
     */
    private Long cancelUserId;

    /**
     * 取消角色：STUDENT/RUNNER/ADMIN/SYSTEM
     */
    private String cancelRole;

    /**
     * 是否申诉中：0否 1是
     */
    private Integer appealFlag;

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
