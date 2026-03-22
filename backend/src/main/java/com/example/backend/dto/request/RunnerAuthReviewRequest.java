package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 跑腿员认证审核请求DTO
 */
@Data
public class RunnerAuthReviewRequest {

    /** 审核结果：1通过 2驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer authStatus;

    /** 驳回原因，驳回时必填 */
    private String rejectReason;
}
