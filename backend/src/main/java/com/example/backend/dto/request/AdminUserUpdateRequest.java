package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员修改用户资料请求DTO
 * <p>
 * 仅允许修改用户基础资料（姓名、昵称、手机号、头像、性别），
 * 禁止修改 username、password、userStatus、roles 等字段。
 * </p>
 */
@Data
public class AdminUserUpdateRequest {

    /** 真实姓名，必填 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 32, message = "真实姓名最长32字")
    private String realName;

    /** 昵称，可选 */
    @Size(max = 32, message = "昵称最长32字")
    private String nickName;

    /** 手机号，可选，填写时必须为11位手机号格式 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 头像地址，可选，填写时必须以http/https开头 */
    @Pattern(regexp = "^https?://.+", message = "头像地址必须以http/https开头")
    private String avatarUrl;

    /** 性别：0未知 1男 2女 */
    private Integer gender;
}
