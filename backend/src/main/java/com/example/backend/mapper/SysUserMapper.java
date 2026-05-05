package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.io.Serializable;
import java.util.List;

/**
 * 系统用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询用户角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Select("SELECT r.id, r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.role_status = 1 AND r.role_status = 1 AND r.is_deleted = 0")
    List<UserRole> selectUserRoles(@Param("userId") Long userId);

    /**
     * 批量查询多个用户的角色列表
     *
     * @param userIds 用户ID列表
     * @return 角色列表（包含userId字段用于分组）
     */
    @Select("<script>" +
            "SELECT ur.user_id AS user_id, r.role_code AS role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id IN " +
            "<foreach item='id' collection='userIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND ur.role_status = 1 AND r.role_status = 1 AND r.is_deleted = 0" +
            "</script>")
    List<UserRoleWithUserId> selectUserRolesByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 用户角色VO（带userId，用于批量查询分组）
     */
    class UserRoleWithUserId implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long userId;
        private String roleCode;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    }

    /**
     * 用户角色VO
     */
    class UserRole implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String roleCode;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }
    }
}
