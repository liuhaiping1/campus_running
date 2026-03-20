package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态流转日志实体类
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
