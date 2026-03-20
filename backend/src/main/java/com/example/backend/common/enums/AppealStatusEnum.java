package com.example.backend.common.enums;

/**
 * 申诉状态枚举
 */
public enum AppealStatusEnum {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    UPHELD(2, "已成立"),
    REJECTED(3, "已驳回"),
    CLOSED(4, "已关闭");

    private final Integer code;
    private final String description;

    AppealStatusEnum(Integer code, String description) {
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
    public static AppealStatusEnum getByCode(Integer code) {
        for (AppealStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
