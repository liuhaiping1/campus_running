package com.example.backend.common.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatusEnum {
    UNPAID(0, "待支付"),
    WAITING_ACCEPT(1, "待接单"),
    ACCEPTED(2, "已接单"),
    CONTACTED(3, "已联系用户"),
    PICKED_UP(4, "已取件"),
    DELIVERING(5, "配送中"),
    DELIVERED(6, "已送达"),
    COMPLETED(7, "已完成"),
    CANCELLED(8, "已取消"),
    CLOSED(9, "已关闭"),
    APPEALING(10, "申诉中");

    private final Integer code;
    private final String description;

    OrderStatusEnum(Integer code, String description) {
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
    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
