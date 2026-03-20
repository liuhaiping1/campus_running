package com.example.backend.common.enums;

/**
 * 结算状态枚举
 */
public enum SettlementStatusEnum {
    PENDING(0, "待结算"),
    SETTLING(1, "结算中"),
    SETTLED(2, "已结算"),
    ROLLED_BACK(3, "已回滚");

    private final Integer code;
    private final String description;

    SettlementStatusEnum(Integer code, String description) {
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
    public static SettlementStatusEnum getByCode(Integer code) {
        for (SettlementStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
