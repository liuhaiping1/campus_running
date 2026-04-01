package com.example.backend.vo;

import com.example.backend.entity.OrderStatusLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态流转日志视图对象
 * <p>
 * 封装订单状态流转日志的展示信息，用于订单详情页展示状态变更轨迹。
 * </p>
 */
@Data
@Builder
public class OrderStatusLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 变更前订单状态
     */
    private Integer beforeStatus;

    /**
     * 变更后订单状态
     */
    private Integer afterStatus;

    /**
     * 触发动作
     */
    private String triggerAction;

    /**
     * 操作人ID，系统任务为0
     */
    private Long operatorUserId;

    /**
     * 操作人角色：STUDENT/RUNNER/ADMIN/SYSTEM
     */
    private String operatorRole;

    /**
     * 状态变更说明
     */
    private String remark;

    /**
     * 请求号或回调编号
     */
    private String requestId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 根据订单状态流转日志实体构建视图对象
     *
     * @param log 订单状态流转日志实体
     * @return 订单状态流转日志视图对象
     */
    public static OrderStatusLogVO from(OrderStatusLog log) {
        return OrderStatusLogVO.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .orderNo(log.getOrderNo())
                .beforeStatus(log.getBeforeStatus())
                .afterStatus(log.getAfterStatus())
                .triggerAction(log.getTriggerAction())
                .operatorUserId(log.getOperatorUserId())
                .operatorRole(log.getOperatorRole())
                .remark(log.getRemark())
                .requestId(log.getRequestId())
                .createTime(log.getCreateTime())
                .build();
    }
}
