package com.example.backend.vo;

import com.example.backend.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户视图对象
 * <p>
 * 包含用户基础资料和角色列表，用于管理端用户列表和详情展示。
 * roles 字段通过 {@code sysUserMapper.selectUserRoles()} 查询，
 * 确保过滤 sys_role.is_deleted 和 role_status 条件。
 * </p>
 */
@Data
public class AdminUserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 昵称 */
    private String nickName;

    /** 手机号 */
    private String phone;

    /** 头像地址 */
    private String avatarUrl;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 用户状态：1正常 2禁用 */
    private Integer userStatus;

    /** 角色列表 */
    private List<String> roles;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 从实体和角色列表构建VO
     *
     * @param user  用户实体
     * @param roles 角色编码列表
     * @return AdminUserVO
     */
    public static AdminUserVO from(SysUser user, List<String> roles) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setNickName(user.getNickName());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setUserStatus(user.getUserStatus());
        vo.setRoles(roles);
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
