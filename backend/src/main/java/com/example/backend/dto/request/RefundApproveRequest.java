package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin refund approval request.
 */
@Data
public class RefundApproveRequest {

    /**
     * Approval refund status, only success or failed is accepted.
     */
    @NotNull(message = "refundStatus cannot be null")
    private Integer refundStatus;

    /**
     * Approval result description.
     */
    @NotBlank(message = "approveResult cannot be blank")
    @Size(max = 255, message = "approveResult length cannot exceed 255")
    private String approveResult;

    /**
     * Validates that refundStatus is a terminal approval status.
     *
     * @return true when refundStatus is success or failed
     */
    @AssertTrue(message = "refundStatus must be 2 or 3")
    public boolean isRefundStatusAllowed() {
        return refundStatus == null || refundStatus == 2 || refundStatus == 3;
    }
}
