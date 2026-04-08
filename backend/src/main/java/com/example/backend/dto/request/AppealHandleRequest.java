package com.example.backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for handling an appeal in the admin console.
 */
@Data
public class AppealHandleRequest {

    /**
     * Final appeal status: 2 upheld, 3 rejected, 4 closed.
     */
    @NotNull(message = "appealStatus cannot be null")
    private Integer appealStatus;

    /**
     * Optional target order status after handling.
     */
    private Integer resultOrderStatus;

    /**
     * Optional responsibility type: 1 to 4.
     */
    private Integer responsibilityType;

    /**
     * Optional refund decision: 0 to 2.
     */
    private Integer refundDecision;

    /**
     * Admin handling result.
     */
    @NotBlank(message = "handleResult cannot be blank")
    @Size(max = 500, message = "handleResult cannot exceed 500 characters")
    private String handleResult;

    /**
     * Validates that appealStatus is one of the allowed final statuses.
     *
     * @return true when appealStatus is allowed
     */
    @AssertTrue(message = "appealStatus must be 2, 3 or 4")
    public boolean isAppealStatusAllowed() {
        return appealStatus == null || appealStatus == 2 || appealStatus == 3 || appealStatus == 4;
    }

    /**
     * Validates the optional responsibility type range.
     *
     * @return true when responsibilityType is absent or valid
     */
    @AssertTrue(message = "responsibilityType must be between 1 and 4")
    public boolean isResponsibilityTypeAllowed() {
        return responsibilityType == null || (responsibilityType >= 1 && responsibilityType <= 4);
    }

    /**
     * Validates the optional refund decision range.
     *
     * @return true when refundDecision is absent or valid
     */
    @AssertTrue(message = "refundDecision must be between 0 and 2")
    public boolean isRefundDecisionAllowed() {
        return refundDecision == null || (refundDecision >= 0 && refundDecision <= 2);
    }
}
