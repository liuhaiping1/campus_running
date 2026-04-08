package com.example.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for submitting an order evaluation.
 */
@Data
public class EvaluationSubmitRequest {

    /** The ID of the order being evaluated. */
    @NotNull
    private Long orderId;

    /** Evaluation score from 1 to 5. */
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    /** Evaluation content, optional, maximum 500 characters. */
    @Size(max = 500)
    private String content;
}
