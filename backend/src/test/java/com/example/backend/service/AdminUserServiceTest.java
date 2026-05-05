package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.AdminUserStatusRequest;
import com.example.backend.dto.request.AdminUserUpdateRequest;
import com.example.backend.entity.SysUser;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.service.impl.AdminUserServiceImpl;
import com.example.backend.vo.AdminUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService 单元测试")
class AdminUserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private SysUser studentUser;
    private SysUser runnerUser;
    private SysUser adminUser;

    @BeforeEach
    void setUp() {
        studentUser = buildUser(1L, "student01", "张三", "小张", "13800000001", 1);
        runnerUser = buildUser(2L, "runner01", "李四", "小李", "13800000002", 1);
        adminUser = buildUser(3L, "admin01", "管理员", "Admin", "13800000003", 1);
    }

    // =========================================================================
    // 列表查询测试
    // =========================================================================

    @Nested
    @DisplayName("用户列表查询")
    class ListTests {

        @Test
        @DisplayName("无筛选条件时应返回所有用户")
        void shouldReturnAllUsersWithoutFilter() {
            // Given
            when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<SysUser> page = inv.getArgument(0);
                        page.setRecords(Arrays.asList(studentUser, runnerUser, adminUser));
                        page.setTotal(3L);
                        return page;
                    });
            when(sysUserMapper.selectUserRolesByUserIds(anyList()))
                    .thenReturn(Arrays.asList(
                            buildUserRoleWithUserId(1L, "STUDENT"),
                            buildUserRoleWithUserId(2L, "RUNNER"),
                            buildUserRoleWithUserId(3L, "ADMIN")));

            // When
            IPage<AdminUserVO> result = adminUserService.list(null, null, null, 1, 10);

            // Then
            assertNotNull(result);
            assertEquals(3L, result.getTotal());
            assertEquals(3, result.getRecords().size());
        }

        @Test
        @DisplayName("应按关键词筛选用户")
        void shouldFilterByKeyword() {
            // Given
            when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<SysUser> page = inv.getArgument(0);
                        page.setRecords(Collections.singletonList(studentUser));
                        page.setTotal(1L);
                        return page;
                    });
            when(sysUserMapper.selectUserRolesByUserIds(anyList()))
                    .thenReturn(Collections.singletonList(
                            buildUserRoleWithUserId(1L, "STUDENT")));

            // When
            IPage<AdminUserVO> result = adminUserService.list("张三", null, null, 1, 10);

            // Then
            assertEquals(1L, result.getTotal());
            assertEquals("张三", result.getRecords().get(0).getRealName());
        }

        @Test
        @DisplayName("应按用户状态筛选")
        void shouldFilterByUserStatus() {
            // Given
            studentUser.setUserStatus(2);
            when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<SysUser> page = inv.getArgument(0);
                        page.setRecords(Collections.singletonList(studentUser));
                        page.setTotal(1L);
                        return page;
                    });
            when(sysUserMapper.selectUserRolesByUserIds(anyList()))
                    .thenReturn(Collections.singletonList(
                            buildUserRoleWithUserId(1L, "STUDENT")));

            // When
            IPage<AdminUserVO> result = adminUserService.list(null, 2, null, 1, 10);

            // Then
            assertEquals(1L, result.getTotal());
            assertEquals(Integer.valueOf(2), result.getRecords().get(0).getUserStatus());
        }

        @Test
        @DisplayName("应按角色编码筛选用户")
        void shouldFilterByRoleCode() {
            // Given: 按RUNNER角色筛选
            SysUserRole runnerRoleMapping = new SysUserRole();
            runnerRoleMapping.setUserId(2L);
            runnerRoleMapping.setRoleCode("RUNNER");
            runnerRoleMapping.setRoleStatus(1);
            when(sysUserRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(runnerRoleMapping));
            when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<SysUser> page = inv.getArgument(0);
                        page.setRecords(Collections.singletonList(runnerUser));
                        page.setTotal(1L);
                        return page;
                    });
            when(sysUserMapper.selectUserRolesByUserIds(anyList()))
                    .thenReturn(Collections.singletonList(
                            buildUserRoleWithUserId(2L, "RUNNER")));

            // When
            IPage<AdminUserVO> result = adminUserService.list(null, null, "RUNNER", 1, 10);

            // Then
            assertEquals(1L, result.getTotal());
            assertTrue(result.getRecords().get(0).getRoles().contains("RUNNER"));
        }
    }

    // =========================================================================
    // 用户详情测试
    // =========================================================================

    @Nested
    @DisplayName("用户详情")
    class DetailTests {

        @Test
        @DisplayName("查询用户详情成功")
        void shouldReturnUserDetail() {
            // Given
            when(sysUserMapper.selectById(1L)).thenReturn(studentUser);
            when(sysUserMapper.selectUserRoles(1L)).thenReturn(Collections.singletonList(buildUserRole("STUDENT")));

            // When
            AdminUserVO result = adminUserService.detail(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("student01", result.getUsername());
            assertEquals("张三", result.getRealName());
            assertTrue(result.getRoles().contains("STUDENT"));
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(sysUserMapper.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.detail(999L),
                    "用户不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
            assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());
        }
    }

    // =========================================================================
    // 修改用户资料测试
    // =========================================================================

    @Nested
    @DisplayName("修改用户资料")
    class UpdateTests {

        @Test
        @DisplayName("修改用户资料成功")
        void shouldUpdateUserSuccessfully() {
            // Given
            when(sysUserMapper.selectById(1L)).thenReturn(studentUser);
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
            when(sysUserMapper.selectUserRoles(1L)).thenReturn(Collections.singletonList(buildUserRole("STUDENT")));

            AdminUserUpdateRequest request = new AdminUserUpdateRequest();
            request.setRealName("张三改名");
            request.setNickName("新昵称");
            request.setPhone("13800000001");
            request.setGender(1);

            // When
            AdminUserVO result = adminUserService.update(1L, request);

            // Then
            assertNotNull(result);
            verify(sysUserMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("用户不存在时修改应抛出异常")
        void shouldThrowWhenUpdateNotFound() {
            // Given
            when(sysUserMapper.selectById(999L)).thenReturn(null);

            AdminUserUpdateRequest request = new AdminUserUpdateRequest();
            request.setRealName("测试");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.update(999L, request),
                    "用户不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("手机号重复时应抛出异常")
        void shouldThrowWhenPhoneDuplicate() {
            // Given
            when(sysUserMapper.selectById(1L)).thenReturn(studentUser);
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            AdminUserUpdateRequest request = new AdminUserUpdateRequest();
            request.setRealName("张三");
            request.setPhone("13800000002");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.update(1L, request),
                    "手机号重复时应抛出 BusinessException");

            assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode());
        }
    }

    // =========================================================================
    // 状态变更测试
    // =========================================================================

    @Nested
    @DisplayName("用户状态变更")
    class StatusTests {

        @Test
        @DisplayName("禁用用户成功")
        void shouldDisableUserSuccessfully() {
            // Given
            when(sysUserMapper.selectById(1L)).thenReturn(studentUser);
            when(sysUserMapper.selectUserRoles(1L)).thenReturn(Collections.singletonList(buildUserRole("STUDENT")));
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

            AdminUserStatusRequest request = new AdminUserStatusRequest();
            request.setUserStatus(2);

            // When
            AdminUserVO result = adminUserService.updateStatus(1L, request, 3L);

            // Then
            assertNotNull(result);
            assertEquals(Integer.valueOf(2), result.getUserStatus());
            verify(sysUserMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("启用用户成功")
        void shouldEnableUserSuccessfully() {
            // Given: 用户当前已禁用
            studentUser.setUserStatus(2);
            when(sysUserMapper.selectById(1L)).thenReturn(studentUser);
            when(sysUserMapper.selectUserRoles(1L)).thenReturn(Collections.singletonList(buildUserRole("STUDENT")));
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

            AdminUserStatusRequest request = new AdminUserStatusRequest();
            request.setUserStatus(1);

            // When
            AdminUserVO result = adminUserService.updateStatus(1L, request, 3L);

            // Then
            assertNotNull(result);
            assertEquals(Integer.valueOf(1), result.getUserStatus());
        }

        @Test
        @DisplayName("禁止管理员禁用自己")
        void shouldThrowWhenDisableSelf() {
            // Given
            when(sysUserMapper.selectById(3L)).thenReturn(adminUser);

            AdminUserStatusRequest request = new AdminUserStatusRequest();
            request.setUserStatus(2);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.updateStatus(3L, request, 3L),
                    "管理员禁用自己时应抛出 BusinessException");

            assertEquals(ErrorCode.CANNOT_DISABLE_SELF.getCode(), exception.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("禁止禁用最后一个有效ADMIN")
        void shouldThrowWhenDisableLastAdmin() {
            // Given
            when(sysUserMapper.selectById(3L)).thenReturn(adminUser);
            when(sysUserMapper.selectUserRoles(3L)).thenReturn(Collections.singletonList(buildUserRole("ADMIN")));
            SysUserRole adminRoleMapping = new SysUserRole();
            adminRoleMapping.setUserId(3L);
            adminRoleMapping.setRoleCode("ADMIN");
            adminRoleMapping.setRoleStatus(1);
            when(sysUserRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(adminRoleMapping));
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            AdminUserStatusRequest request = new AdminUserStatusRequest();
            request.setUserStatus(2);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.updateStatus(3L, request, 1L),
                    "禁用最后一个ADMIN时应抛出 BusinessException");

            assertEquals(ErrorCode.CANNOT_DISABLE_LAST_ADMIN.getCode(), exception.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("非法userStatus应抛出异常")
        void shouldThrowWhenInvalidUserStatus() {
            // Given
            when(sysUserMapper.selectById(1L)).thenReturn(studentUser);

            AdminUserStatusRequest request = new AdminUserStatusRequest();
            request.setUserStatus(5);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.updateStatus(1L, request, 3L),
                    "非法状态值应抛出 BusinessException");

            assertEquals(ErrorCode.INVALID_USER_STATUS.getCode(), exception.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private SysUser buildUser(Long id, String username, String realName,
                              String nickName, String phone, int userStatus) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setNickName(nickName);
        user.setPhone(phone);
        user.setGender(0);
        user.setUserStatus(userStatus);
        user.setCreateTime(LocalDateTime.now().minusDays(30));
        user.setUpdateTime(LocalDateTime.now().minusDays(1));
        return user;
    }

    private SysUserMapper.UserRole buildUserRole(String roleCode) {
        SysUserMapper.UserRole role = new SysUserMapper.UserRole();
        role.setId(1L);
        role.setRoleCode(roleCode);
        return role;
    }

    private SysUserMapper.UserRoleWithUserId buildUserRoleWithUserId(Long userId, String roleCode) {
        SysUserMapper.UserRoleWithUserId role = new SysUserMapper.UserRoleWithUserId();
        role.setUserId(userId);
        role.setRoleCode(roleCode);
        return role;
    }
}
