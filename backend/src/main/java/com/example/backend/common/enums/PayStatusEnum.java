package com.example.backend.common.enums;

/**
 * 支付状态枚举
 */
public enum PayStatusEnum {
    UNPAID(0, "未支付"),
    PAYING(1, "支付中"),
    PAID(2, "支付成功"),
    REFUNDING(3, "退款中"),
    REFUNDED(4, "已退款"),
    PAYMENT_CLOSED(5, "支付关闭"),
    PARTIAL_REFUND(6, "部分退款");

    private final Integer code;
    private final String description;

    PayStatusEnum(Integer code, String description) {
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
    public static PayStatusEnum getByCode(Integer code) {
        for (PayStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
