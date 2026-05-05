package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private String payNo;

    private String payChannel;

    private BigDecimal payAmount;

    /**
     * 支付单过期时间
     */
    private LocalDateTime expireTime;

    private Integer payStatus;

    private String tradeNo;

    private String callbackContent;

    private Integer callbackStatus;

    private LocalDateTime callbackTime;

    private BigDecimal refundAmount;

    private LocalDateTime payTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer isDeleted;
}
