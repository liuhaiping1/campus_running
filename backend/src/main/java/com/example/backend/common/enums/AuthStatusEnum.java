package com.example.backend.common.enums;

/**
 * 认证状态枚举
 */
public enum AuthStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核驳回"),
    EXPIRED(3, "已失效");

    private final Integer code;
    private final String description;

    AuthStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static AuthStatusEnum getByCode(Integer code) {
        for (AuthStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
