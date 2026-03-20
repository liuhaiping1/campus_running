package com.example.backend.common.enums;

/**
 * 角色编码枚举
 */
public enum RoleCodeEnum {
    STUDENT("STUDENT", "学生用户"),
    RUNNER("RUNNER", "校园跑腿员"),
    ADMIN("ADMIN", "系统管理员");

    private final String code;
    private final String description;

    RoleCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static RoleCodeEnum getByCode(String code) {
        for (RoleCodeEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
