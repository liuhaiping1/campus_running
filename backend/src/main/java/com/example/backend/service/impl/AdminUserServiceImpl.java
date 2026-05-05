package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.RoleCodeEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.AdminUserStatusRequest;
import com.example.backend.dto.request.AdminUserUpdateRequest;
import com.example.backend.entity.SysUser;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.service.AdminUserService;
import com.example.backend.vo.AdminUserVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务实现类
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public AdminUserServiceImpl(SysUserMapper sysUserMapper,
                                SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public IPage<AdminUserVO> list(String keyword, Integer userStatus, String roleCode,
                                   int pageNum, int pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (userStatus != null) {
            wrapper.eq(SysUser::getUserStatus, userStatus);
        }

        if (roleCode != null && !roleCode.trim().isEmpty()) {
            List<Long> userIds = getUserIdsByRoleCode(roleCode.trim());
            if (userIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            wrapper.in(SysUser::getId, userIds);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, trimmed)
                    .or().like(SysUser::getRealName, trimmed)
                    .or().like(SysUser::getNickName, trimmed)
                    .or().like(SysUser::getPhone, trimmed));
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> resultPage = sysUserMapper.selectPage(page, wrapper);

        // 批量查询本页所有用户的角色，避免N+1
        List<Long> pageUserIds = resultPage.getRecords().stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> rolesMap = batchGetRoleCodes(pageUserIds);

        Page<AdminUserVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
                .map(user -> AdminUserVO.from(user,
                        rolesMap.getOrDefault(user.getId(), Collections.emptyList())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public AdminUserVO detail(Long id) {
        SysUser user = requireUser(id);
        return AdminUserVO.from(user, getUserRoleCodes(id));
    }

    @Override
    public AdminUserVO update(Long id, AdminUserUpdateRequest request) {
        SysUser user = requireUser(id);

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, request.getPhone())
                    .ne(SysUser::getId, id);
            if (sysUserMapper.selectCount(phoneWrapper) > 0) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }

        user.setRealName(request.getRealName());
        user.setNickName(request.getNickName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());

        sysUserMapper.updateById(user);
        return AdminUserVO.from(user, getUserRoleCodes(id));
    }

    @Override
    public AdminUserVO updateStatus(Long id, AdminUserStatusRequest request, Long operatorId) {
        SysUser user = requireUser(id);

        int newStatus = request.getUserStatus();
        if (newStatus != 1 && newStatus != 2) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS);
        }

        // 仅在禁用时执行以下校验
        if (newStatus == 2) {
            if (id.equals(operatorId)) {
                throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
            }
            List<String> roles = getUserRoleCodes(id);
            if (roles.contains(RoleCodeEnum.ADMIN.getCode()) && countActiveAdmins() <= 1) {
                throw new BusinessException(ErrorCode.CANNOT_DISABLE_LAST_ADMIN);
            }
            // 复用已查询的roles，避免重复查询
            user.setUserStatus(newStatus);
            sysUserMapper.updateById(user);
            return AdminUserVO.from(user, roles);
        }

        user.setUserStatus(newStatus);
        sysUserMapper.updateById(user);
        return AdminUserVO.from(user, getUserRoleCodes(id));
    }

    /**
     * 查询单个用户的角色编码列表
     */
    private List<String> getUserRoleCodes(Long userId) {
        return sysUserMapper.selectUserRoles(userId).stream()
                .map(SysUserMapper.UserRole::getRoleCode)
                .collect(Collectors.toList());
    }

    /**
     * 批量查询多个用户的角色编码，返回userId -> roleCodes映射
     */
    private Map<Long, List<String>> batchGetRoleCodes(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectUserRolesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(
                        SysUserMapper.UserRoleWithUserId::getUserId,
                        Collectors.mapping(SysUserMapper.UserRoleWithUserId::getRoleCode,
                                Collectors.toList())));
    }

    /**
     * 查询拥有指定角色的用户ID列表
     */
    private List<Long> getUserIdsByRoleCode(String roleCode) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleCode, roleCode)
                .eq(SysUserRole::getRoleStatus, 1);
        return sysUserRoleMapper.selectList(wrapper).stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 统计当前有效的ADMIN用户数量
     */
    private long countActiveAdmins() {
        List<Long> adminUserIds = getUserIdsByRoleCode(RoleCodeEnum.ADMIN.getCode());
        if (adminUserIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getId, adminUserIds)
                .eq(SysUser::getUserStatus, 1);
        return sysUserMapper.selectCount(wrapper);
    }

    /**
     * 查询用户，不存在则抛异常
     */
    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
