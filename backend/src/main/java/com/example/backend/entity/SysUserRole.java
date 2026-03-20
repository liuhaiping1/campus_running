package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 */
@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 角色编号
     */
    private Long roleId;

    /**
     * 角色编码冗余
     */
    private String roleCode;

    /**
     * 授权来源：1注册默认 2认证通过 3管理员授予
     */
    private Integer grantSource;

    /**
     * 授权时间
     */
    private LocalDateTime grantTime;

    /**
     * 过期时间，NULL表示长期有效
     */
    private LocalDateTime expireTime;

    /**
     * 角色状态：1有效 2停用 3过期
     */
    private Integer roleStatus;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 逻辑删除标记
     */
    private Integer isDeleted;
}
