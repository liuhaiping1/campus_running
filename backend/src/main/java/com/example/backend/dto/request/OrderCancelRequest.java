package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单请求
 */
@Data
public class OrderCancelRequest {

    /**
     * 取消原因
     */
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 200, message = "取消原因最长200字")
    private String cancelReason;
}