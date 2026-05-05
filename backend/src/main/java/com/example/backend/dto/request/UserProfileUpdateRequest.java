package com.example.backend.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料修改请求
 */
@Data
public class UserProfileUpdateRequest {

    /**
     * 真实姓名
     */
    @Size(max = 32, message = "真实姓名长度不能超过32位")
    private String realName;

    /**
     * 昵称
     */
    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickName;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 头像地址
     */
    @Size(max = 255, message = "头像地址长度不能超过255位")
    private String avatarUrl;

    /**
     * 性别：0未知 1男 2女
     */
    private Integer gender;
}
