package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * View object for appeal records.
 */
@Data
@Builder
public class AppealRecordVO {

    /**
     * Appeal record ID.
     */
    private Long id;

    /**
     * Order ID.
     */
    private Long orderId;

    /**
     * Appeal number.
     */
    private String appealNo;

    /**
     * Applicant user ID.
     */
    private Long applyUserId;

    /**
     * Applicant role.
     */
    private String applyRole;

    /**
     * Appeal type.
     */
    private Integer appealType;

    /**
     * Appeal content.
     */
    private String appealContent;

    /**
     * Evidence URL list or JSON string.
     */
    private String evidenceUrls;

    /**
     * Appeal status.
     */
    private Integer appealStatus;

    /**
     * Order status before appeal.
     */
    private Integer beforeOrderStatus;

    /**
     * Resulting order status after handling.
     */
    private Integer resultOrderStatus;

    /**
     * Responsibility type.
     */
    private Integer responsibilityType;

    /**
     * Refund decision.
     */
    private Integer refundDecision;

    /**
     * Admin handler ID.
     */
    private Long handleAdminId;

    /**
     * Handling result.
     */
    private String handleResult;

    /**
     * Handling time.
     */
    private LocalDateTime handleTime;

    /**
     * Creation time.
     */
    private LocalDateTime createTime;

    /**
     * Update time.
     */
    private LocalDateTime updateTime;
}
