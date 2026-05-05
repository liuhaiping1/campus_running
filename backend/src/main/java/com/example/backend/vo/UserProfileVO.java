package com.example.backend.vo;

import com.example.backend.entity.SysUser;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户资料响应视图对象
 */
@Data
@Builder
public class UserProfileVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 性别：0未知 1男 2女
     */
    private Integer gender;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 根据用户实体和角色列表构建响应对象
     *
     * @param user  用户实体
     * @param roles 角色编码列表
     * @return 用户资料响应对象
     */
    public static UserProfileVO from(SysUser user, List<String> roles) {
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .nickName(user.getNickName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .roles(roles)
                .createTime(user.getCreateTime())
                .build();
    }
}
