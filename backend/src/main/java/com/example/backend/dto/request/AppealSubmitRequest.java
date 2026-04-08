package com.example.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for submitting an order appeal.
 */
@Data
public class AppealSubmitRequest {

    /**
     * Appealed order ID.
     */
    @NotNull(message = "orderId cannot be null")
    private Long orderId;

    /**
     * Appeal type: 1 cancel dispute, 2 fulfillment dispute, 3 refund dispute.
     */
    @NotNull(message = "appealType cannot be null")
    @Min(value = 1, message = "appealType must be between 1 and 3")
    @Max(value = 3, message = "appealType must be between 1 and 3")
    private Integer appealType;

    /**
     * Appeal content.
     */
    @NotBlank(message = "appealContent cannot be blank")
    @Size(max = 1000, message = "appealContent cannot exceed 1000 characters")
    private String appealContent;

    /**
     * Evidence URL list or JSON string.
     */
    @Size(max = 1000, message = "evidenceUrls cannot exceed 1000 characters")
    private String evidenceUrls;
}
