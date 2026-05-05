package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    /**
     * 关联支付单号快照
     */
    private String payNo;

    private String refundNo;

    private Long applyUserId;

    private Integer refundType;

    private BigDecimal refundAmount;

    private String refundReason;

    private Integer refundStatus;

    private String thirdRefundNo;

    private String requestId;

    private String callbackContent;

    private Integer retryCount;

    private LocalDateTime lastRetryTime;

    private Long approveAdminId;

    private String approveResult;

    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer isDeleted;
}
