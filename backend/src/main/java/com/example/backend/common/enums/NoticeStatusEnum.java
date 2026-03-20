package com.example.backend.common.enums;

/**
 * 公告状态枚举
 */
public enum NoticeStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下架");

    private final Integer code;
    private final String description;

    NoticeStatusEnum(Integer code, String description) {
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
    public static NoticeStatusEnum getByCode(Integer code) {
        for (NoticeStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
