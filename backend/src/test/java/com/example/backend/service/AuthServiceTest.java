package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.entity.SysRole;
import com.example.backend.entity.SysUser;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.SysRoleMapper;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.security.JwtTokenUtil;
import com.example.backend.service.impl.AuthServiceImpl;
import com.example.backend.vo.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试类
 * <p>
 * 使用 Mockito 对 {@link AuthServiceImpl} 进行单元测试，
 * 覆盖注册和登录两大核心业务流程的所有正常路径和异常路径。
 * <p>
 * 测试采用 @Nested 分组，将注册和登录相关测试分别组织，
 * 每组包含正常场景和各类异常场景，确保业务逻辑的完整性验证。
 *
 * @author campus_running
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    // =========================================================================
    // Mock 依赖
    // =========================================================================

    /** Mock 系统用户 Mapper */
    @Mock
    private SysUserMapper sysUserMapper;

    /** Mock 系统角色 Mapper */
    @Mock
    private SysRoleMapper sysRoleMapper;

    /** Mock 用户角色关联 Mapper */
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    /** Mock 密码编码器 */
    @Mock
    private PasswordEncoder passwordEncoder;

    /** Mock JWT 令牌工具类 */
    @Mock
    private JwtTokenUtil jwtTokenUtil;

    /** 自动注入 Mock 依赖的被测对象 */
    @InjectMocks
    private AuthServiceImpl authService;

    // =========================================================================
    // 通用测试数据
    // =========================================================================

    /** 测试用注册请求 */
    private RegisterRequest registerRequest;

    /** 测试用登录请求 */
    private LoginRequest loginRequest;

    /** 测试用已持久化的用户对象 */
    private SysUser savedUser;

    /** 测试用 STUDENT 角色对象 */
    private SysRole studentRole;

    /**
     * 每个测试方法执行前的初始化操作
     * <p>
     * 构造通用的注册请求、登录请求、用户实体和角色实体，
     * 确保每个测试都有干净的测试数据。
     */
    @BeforeEach
    void setUp() {
        // 构建注册请求
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setRealName("测试用户");
        registerRequest.setPhone("13800138000");

        // 构建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // 构建已保存的用户（模拟 insert 后获得 ID）
        savedUser = new SysUser();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setPassword("encodedPassword");
        savedUser.setRealName("测试用户");
        savedUser.setPhone("13800138000");
        savedUser.setUserStatus(1);

        // 构建 STUDENT 角色
        studentRole = new SysRole();
        studentRole.setId(10L);
        studentRole.setRoleCode("STUDENT");
        studentRole.setRoleStatus(1);
    }

    // =========================================================================
    // 注册测试组
    // =========================================================================

    /**
     * 注册功能测试组
     * <p>
     * 覆盖注册的四种核心场景：
     * <ol>
     *   <li>正常注册成功 —— 用户名和手机号均未占用，STUDENT角色存在</li>
     *   <li>用户名已存在异常</li>
     *   <li>手机号已存在异常</li>
     *   <li>STUDENT角色不存在异常</li>
     * </ol>
     */
    @Nested
    @DisplayName("注册")
    class RegisterTests {

        /**
         * 测试正常注册成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>selectCount 被调用两次（分别校验用户名和手机号）</li>
         *   <li>用户密码经过 encode 加密后再保存</li>
         *   <li>用户状态设置为 1（正常）</li>
         *   <li>正确查询 STUDENT 角色</li>
         *   <li>正确为用户授予 STUDENT 角色（grantSource=1 表示注册默认）</li>
         *   <li>返回值为用户 ID</li>
         * </ul>
         */
        @Test
        @DisplayName("应成功注册新用户")
        void shouldRegisterSuccessfully() {
            // Given: 用户名和手机号均不存在
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            // 密码编码（不会返回原文）
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
            // insert 成功后设置用户 ID
            doAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(1L);
                return 1;
            }).when(sysUserMapper).insert(any(SysUser.class));
            // STUDENT 角色存在
            when(sysRoleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(studentRole);
            // 角色关联插入成功
            when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

            // When: 执行注册
            Long userId = authService.register(registerRequest);

            // Then: 验证用户名和手机号校验各执行一次
            verify(sysUserMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));

            // 验证插入的用户信息正确
            ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
            verify(sysUserMapper).insert(userCaptor.capture());
            SysUser insertedUser = userCaptor.getValue();
            assertEquals("testuser", insertedUser.getUsername(), "用户名应一致");
            assertEquals("encodedPassword", insertedUser.getPassword(), "密码应经过加密");
            assertEquals("测试用户", insertedUser.getRealName(), "真实姓名应一致");
            assertEquals("13800138000", insertedUser.getPhone(), "手机号应一致");
            assertEquals(Integer.valueOf(1), insertedUser.getUserStatus(), "用户状态应为正常(1)");

            // 验证正确查询了 STUDENT 角色
            verify(sysRoleMapper).selectOne(any(LambdaQueryWrapper.class));

            // 验证正确分配了 STUDENT 角色
            ArgumentCaptor<SysUserRole> roleCaptor = ArgumentCaptor.forClass(SysUserRole.class);
            verify(sysUserRoleMapper).insert(roleCaptor.capture());
            SysUserRole insertedRole = roleCaptor.getValue();
            assertEquals(1L, insertedRole.getUserId(), "角色关联的用户ID应一致");
            assertEquals(Long.valueOf(10L), insertedRole.getRoleId(), "角色ID应为STUDENT角色的ID");
            assertEquals("STUDENT", insertedRole.getRoleCode(), "角色编码应为STUDENT");
            assertEquals(Integer.valueOf(1), insertedRole.getGrantSource(), "授权来源应为注册默认(1)");
            assertEquals(Integer.valueOf(1), insertedRole.getRoleStatus(), "角色状态应为有效(1)");
            assertNotNull(insertedRole.getGrantTime(), "授权时间不应为空");

            // 验证返回用户ID
            assertEquals(Long.valueOf(1L), userId, "应返回新用户的ID");
        }

        /**
         * 测试用户名已存在时抛出异常
         * <p>
         * 当 selectCount 针对用户名的查询返回大于0时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#USERNAME_EXISTS}。
         */
        @Test
        @DisplayName("用户名已存在时应抛出 USERNAME_EXISTS 异常")
        void shouldThrowWhenUsernameExists() {
            // Given: 用户名已存在
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(registerRequest),
                    "用户名已存在时应抛出 BusinessException");

            assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), exception.getCode(),
                    "错误码应为 USERNAME_EXISTS(2001)");
            assertEquals(ErrorCode.USERNAME_EXISTS.getMessage(), exception.getMessage(),
                    "错误信息应为: 用户名已存在");

            // 验证未执行后续的 insert 操作
            verify(sysUserMapper, never()).insert(any(SysUser.class));
            verify(sysRoleMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        }

        /**
         * 测试手机号已存在时抛出异常
         * <p>
         * 当用户名未占用但手机号已被注册时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#PHONE_EXISTS}。
         */
        @Test
        @DisplayName("手机号已存在时应抛出 PHONE_EXISTS 异常")
        void shouldThrowWhenPhoneExists() {
            // Given: 用户名不存在，但手机号已存在
            // 第一次调用（用户名校验）返回0，第二次调用（手机号校验）返回1
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)   // username 不存在
                    .thenReturn(1L);  // phone 已存在

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(registerRequest),
                    "手机号已存在时应抛出 BusinessException");

            assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode(),
                    "错误码应为 PHONE_EXISTS(2002)");
            assertEquals(ErrorCode.PHONE_EXISTS.getMessage(), exception.getMessage(),
                    "错误信息应为: 手机号已存在");

            // 验证 username 和 phone 校验各执行一次，但未执行后续 insert
            verify(sysUserMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
            verify(sysUserMapper, never()).insert(any(SysUser.class));
        }

        /**
         * 测试 STUDENT 角色不存在时抛出系统异常
         * <p>
         * 当用户名和手机号均未占用，但查询 STUDENT 角色返回 null 时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#SYSTEM_ERROR}，
         * 并附带自定义错误信息。
         */
        @Test
        @DisplayName("STUDENT 角色不存在时应抛出 SYSTEM_ERROR 异常")
        void shouldThrowWhenStudentRoleNotFound() {
            // Given: 用户名和手机号均不冲突
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            // 密码编码
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
            // 用户插入成功
            doAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(1L);
                return 1;
            }).when(sysUserMapper).insert(any(SysUser.class));
            // STUDENT 角色不存在
            when(sysRoleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(registerRequest),
                    "STUDENT角色不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode(),
                    "错误码应为 SYSTEM_ERROR(1000)");
            assertTrue(exception.getMessage().contains("STUDENT"),
                    "错误信息应包含: STUDENT");

            // 验证用户已创建，但角色未分配
            verify(sysUserMapper).insert(any(SysUser.class));
            verify(sysRoleMapper).selectOne(any(LambdaQueryWrapper.class));
            verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
        }
    }

    // =========================================================================
    // 登录测试组
    // =========================================================================

    /**
     * 登录功能测试组
     * <p>
     * 覆盖登录的五种核心场景：
     * <ol>
     *   <li>正常登录成功 —— 用户存在、密码正确、账号正常、角色存在</li>
     *   <li>用户不存在异常</li>
     *   <li>账号被禁用异常</li>
     *   <li>密码错误异常</li>
     *   <li>多角色用户登录 —— 验证所有角色均返回</li>
     * </ol>
     */
    @Nested
    @DisplayName("登录")
    class LoginTests {

        /**
         * 测试正常登录成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>根据用户名查询用户</li>
         *   <li>验证密码匹配</li>
         *   <li>更新最后登录时间</li>
         *   <li>查询用户角色列表</li>
         *   <li>生成 JWT 令牌（包含正确的 userId、username、roles）</li>
         *   <li>返回完整的 LoginResponse（token、userId、username、realName、roles）</li>
         * </ul>
         */
        @Test
        @DisplayName("应成功登录并返回 JWT 令牌和用户信息")
        void shouldLoginSuccessfully() {
            // Given: 用户存在
            when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(savedUser);
            // 密码匹配
            when(passwordEncoder.matches(loginRequest.getPassword(), savedUser.getPassword()))
                    .thenReturn(true);
            // 更新最后登录时间成功
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
            // 角色列表（单角色 STUDENT）
            SysUserMapper.UserRole userRole = new SysUserMapper.UserRole();
            userRole.setId(10L);
            userRole.setRoleCode("STUDENT");
            List<SysUserMapper.UserRole> userRoles = Collections.singletonList(userRole);
            when(sysUserMapper.selectUserRoles(savedUser.getId())).thenReturn(userRoles);
            // JWT 令牌生成
            when(jwtTokenUtil.generateToken(anyLong(), anyString(), anyList()))
                    .thenReturn("test.jwt.token");

            // When: 执行登录
            LoginResponse response = authService.login(loginRequest);

            // Then: 验证查询用户
            verify(sysUserMapper).selectOne(any(LambdaQueryWrapper.class));

            // 验证密码校验
            verify(passwordEncoder).matches(loginRequest.getPassword(), savedUser.getPassword());

            // 验证最后登录时间已更新
            ArgumentCaptor<SysUser> updatedUserCaptor = ArgumentCaptor.forClass(SysUser.class);
            verify(sysUserMapper).updateById(updatedUserCaptor.capture());
            SysUser updatedUser = updatedUserCaptor.getValue();
            assertEquals(savedUser.getId(), updatedUser.getId(), "更新的用户ID应一致");
            assertNotNull(updatedUser.getLastLoginTime(), "最后登录时间应已设置");

            // 验证查询了用户角色
            verify(sysUserMapper).selectUserRoles(savedUser.getId());

            // 验证 JWT 令牌生成参数正确
            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
            verify(jwtTokenUtil).generateToken(
                    userIdCaptor.capture(),
                    usernameCaptor.capture(),
                    rolesCaptor.capture()
            );
            assertEquals(Long.valueOf(1L), userIdCaptor.getValue(), "JWT中的用户ID应一致");
            assertEquals("testuser", usernameCaptor.getValue(), "JWT中的用户名应一致");
            assertEquals(Collections.singletonList("STUDENT"), rolesCaptor.getValue(),
                    "JWT中的角色列表应包含STUDENT");

            // 验证返回的 LoginResponse
            assertNotNull(response, "LoginResponse不应为null");
            assertEquals("test.jwt.token", response.getToken(), "令牌应一致");
            assertEquals(Long.valueOf(1L), response.getUserId(), "用户ID应一致");
            assertEquals("testuser", response.getUsername(), "用户名应一致");
            assertEquals("测试用户", response.getRealName(), "真实姓名应一致");
            assertNotNull(response.getRoles(), "角色列表不应为null");
            assertEquals(1, response.getRoles().size(), "应有1个角色");
            assertTrue(response.getRoles().contains("STUDENT"), "应包含STUDENT角色");
        }

        /**
         * 测试用户不存在时抛出异常
         * <p>
         * 当根据用户名查询不到用户时，
         * 应抛出 {@link BusinessException} 且错误码为
         * {@link ErrorCode#USERNAME_OR_PASSWORD_ERROR}。
         */
        @Test
        @DisplayName("用户不存在时应抛出 USERNAME_OR_PASSWORD_ERROR 异常")
        void shouldThrowWhenUserNotFound() {
            // Given: 用户不存在
            when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest),
                    "用户不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), exception.getCode(),
                    "错误码应为 USERNAME_OR_PASSWORD_ERROR(2003)");
            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getMessage(), exception.getMessage(),
                    "错误信息应为: 用户名或密码错误");

            // 验证未执行密码校验等后续操作
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
            verify(jwtTokenUtil, never()).generateToken(anyLong(), anyString(), anyList());
        }

        /**
         * 测试账号被禁用时抛出异常
         * <p>
         * 当用户存在但 userStatus 不为 1（即被禁用）时，
         * 应抛出 {@link BusinessException} 且错误码为
         * {@link ErrorCode#ACCOUNT_DISABLED}。
         */
        @Test
        @DisplayName("账号被禁用时应抛出 ACCOUNT_DISABLED 异常")
        void shouldThrowWhenAccountDisabled() {
            // Given: 用户存在但被禁用
            SysUser disabledUser = new SysUser();
            disabledUser.setId(2L);
            disabledUser.setUsername("disableduser");
            disabledUser.setPassword("encodedPassword");
            disabledUser.setUserStatus(2); // 禁用状态
            when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(disabledUser);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest),
                    "账号被禁用时应抛出 BusinessException");

            assertEquals(ErrorCode.ACCOUNT_DISABLED.getCode(), exception.getCode(),
                    "错误码应为 ACCOUNT_DISABLED(2004)");
            assertEquals(ErrorCode.ACCOUNT_DISABLED.getMessage(), exception.getMessage(),
                    "错误信息应为: 账号已被禁用");

            // 验证未执行密码校验等后续操作
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
            verify(jwtTokenUtil, never()).generateToken(anyLong(), anyString(), anyList());
        }

        /**
         * 测试密码错误时抛出异常
         * <p>
         * 当用户存在且状态正常，但密码不匹配时，
         * 应抛出 {@link BusinessException} 且错误码为
         * {@link ErrorCode#USERNAME_OR_PASSWORD_ERROR}。
         */
        @Test
        @DisplayName("密码错误时应抛出 USERNAME_OR_PASSWORD_ERROR 异常")
        void shouldThrowWhenPasswordWrong() {
            // Given: 用户存在，状态正常，但密码不匹配
            when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(savedUser);
            when(passwordEncoder.matches(loginRequest.getPassword(), savedUser.getPassword()))
                    .thenReturn(false);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest),
                    "密码错误时应抛出 BusinessException");

            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), exception.getCode(),
                    "错误码应为 USERNAME_OR_PASSWORD_ERROR(2003)");
            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getMessage(), exception.getMessage(),
                    "错误信息应为: 用户名或密码错误");

            // 验证密码校验已执行，但未执行后续操作
            verify(passwordEncoder).matches(loginRequest.getPassword(), savedUser.getPassword());
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
            verify(jwtTokenUtil, never()).generateToken(anyLong(), anyString(), anyList());
        }

        /**
         * 测试拥有多个角色的用户登录成功
         * <p>
         * 当用户同时拥有 STUDENT 和 RUNNER 两个角色时，
         * 验证 LoginResponse 中 roles 包含这两个角色，
         * 且 JWT 令牌生成时传入了正确的角色列表。
         */
        @Test
        @DisplayName("多角色用户登录应返回所有角色")
        void shouldReturnMultipleRoles() {
            // Given: 用户存在且拥有 STUDENT 和 RUNNER 两个角色
            when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(savedUser);
            when(passwordEncoder.matches(loginRequest.getPassword(), savedUser.getPassword()))
                    .thenReturn(true);
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

            // 构建双角色
            SysUserMapper.UserRole studentRole = new SysUserMapper.UserRole();
            studentRole.setId(10L);
            studentRole.setRoleCode("STUDENT");

            SysUserMapper.UserRole runnerRole = new SysUserMapper.UserRole();
            runnerRole.setId(20L);
            runnerRole.setRoleCode("RUNNER");

            List<SysUserMapper.UserRole> userRoles = Arrays.asList(studentRole, runnerRole);
            when(sysUserMapper.selectUserRoles(savedUser.getId())).thenReturn(userRoles);

            // JWT 令牌生成
            when(jwtTokenUtil.generateToken(anyLong(), anyString(), anyList()))
                    .thenReturn("multi.role.jwt.token");

            // When: 执行登录
            LoginResponse response = authService.login(loginRequest);

            // Then: 验证 JWT 生成时传入了两个角色
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
            verify(jwtTokenUtil).generateToken(anyLong(), anyString(), rolesCaptor.capture());
            List<String> capturedRoles = rolesCaptor.getValue();
            assertEquals(2, capturedRoles.size(), "应有2个角色");
            assertTrue(capturedRoles.contains("STUDENT"), "应包含STUDENT角色");
            assertTrue(capturedRoles.contains("RUNNER"), "应包含RUNNER角色");

            // 验证返回的 LoginResponse 包含两个角色
            assertNotNull(response, "LoginResponse不应为null");
            assertEquals("multi.role.jwt.token", response.getToken(), "令牌应一致");
            assertEquals(Long.valueOf(1L), response.getUserId(), "用户ID应一致");
            assertEquals("testuser", response.getUsername(), "用户名应一致");
            assertEquals("测试用户", response.getRealName(), "真实姓名应一致");
            assertNotNull(response.getRoles(), "角色列表不应为null");
            assertEquals(2, response.getRoles().size(), "应有2个角色");
            assertTrue(response.getRoles().contains("STUDENT"), "应包含STUDENT角色");
            assertTrue(response.getRoles().contains("RUNNER"), "应包含RUNNER角色");

            // 验证最后登录时间已更新
            verify(sysUserMapper).updateById(any(SysUser.class));
        }
    }
}
