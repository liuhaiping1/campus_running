package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员变更用户状态请求DTO
 * <p>
 * 用于启用或禁用用户账号。
 * userStatus 只允许传 1（正常）或 2（禁用）。
 * </p>
 */
@Data
public class AdminUserStatusRequest {

    /** 用户状态：1正常 2禁用 */
    @NotNull(message = "用户状态不能为空")
    private Integer userStatus;
}
