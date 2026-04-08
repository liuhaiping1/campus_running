package com.example.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Refund record view object for admin pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecordVO {

    /**
     * Refund record id.
     */
    private Long id;

    /**
     * Related order id.
     */
    private Long orderId;

    /**
     * Refund number.
     */
    private String refundNo;

    /**
     * Applicant user id.
     */
    private Long applyUserId;

    /**
     * Refund type.
     */
    private Integer refundType;

    /**
     * Refund amount.
     */
    private BigDecimal refundAmount;

    /**
     * Refund reason.
     */
    private String refundReason;

    /**
     * Refund status.
     */
    private Integer refundStatus;

    /**
     * Idempotent request id.
     */
    private String requestId;

    /**
     * Approval admin id.
     */
    private Long approveAdminId;

    /**
     * Approval result.
     */
    private String approveResult;

    /**
     * Refund success time.
     */
    private LocalDateTime refundTime;

    /**
     * Record creation time.
     */
    private LocalDateTime createTime;
}
