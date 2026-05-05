package com.example.backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.PasswordChangeRequest;
import com.example.backend.dto.request.UserProfileUpdateRequest;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.impl.UserProfileServiceImpl;
import com.example.backend.vo.RunnerAuthProfileVO;
import com.example.backend.vo.RunnerCenterOverviewVO;
import com.example.backend.vo.UserCenterOverviewVO;
import com.example.backend.vo.UserProfileVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户个人中心服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService 单元测试")
class UserProfileServiceTest {

    private static final Long USER_ID = 100L;

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private ErrandOrderMapper errandOrderMapper;
    @Mock
    private StationMessageMapper stationMessageMapper;
    @Mock
    private RunnerAuthMapper runnerAuthMapper;
    @Mock
    private RunnerIncomeRecordMapper runnerIncomeRecordMapper;
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    /**
     * 初始化 MyBatis-Plus 表元信息
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, ErrandOrder.class);
        TableInfoHelper.initTableInfo(assistant, StationMessage.class);
        TableInfoHelper.initTableInfo(assistant, RunnerAuth.class);
        TableInfoHelper.initTableInfo(assistant, RunnerIncomeRecord.class);
        TableInfoHelper.initTableInfo(assistant, OrderEvaluation.class);
    }

    /**
     * 查询个人资料成功
     */
    @Test
    @DisplayName("getProfile 查询个人资料成功")
    void shouldReturnProfileWhenUserExists() {
        SysUser user = mockUser(USER_ID);
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        mockUserRoles(USER_ID, List.of("STUDENT"));

        UserProfileVO profile = userProfileService.getProfile(USER_ID);

        assertNotNull(profile);
        assertEquals(USER_ID, profile.getId());
        assertEquals("testuser", profile.getUsername());
        assertEquals("张三", profile.getRealName());
        assertEquals("13800138000", profile.getPhone());
        assertEquals(1, profile.getRoles().size());
        assertEquals("STUDENT", profile.getRoles().get(0));
    }

    /**
     * 修改个人资料成功
     */
    @Test
    @DisplayName("updateProfile 修改个人资料成功")
    void shouldUpdateProfileSuccessfully() {
        SysUser user = mockUser(USER_ID);
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        mockUserRoles(USER_ID, List.of("STUDENT"));

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setRealName("李四");
        request.setNickName("小四");
        request.setPhone("13900139000");
        request.setGender(2);

        UserProfileVO result = userProfileService.updateProfile(USER_ID, request);

        assertNotNull(result);
        assertEquals("李四", result.getRealName());
        assertEquals("小四", result.getNickName());
        assertEquals("13900139000", result.getPhone());
        assertEquals(2, result.getGender());
        verify(sysUserMapper).updateById(any(SysUser.class));
    }

    /**
     * 修改手机号已被其他用户使用时抛出异常
     */
    @Test
    @DisplayName("updateProfile 手机号已存在时抛出异常")
    void shouldThrowWhenPhoneAlreadyExists() {
        SysUser user = mockUser(USER_ID);
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        // 手机号已被其他人使用
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setPhone("13900139000");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userProfileService.updateProfile(USER_ID, request));

        assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode());
    }

    /**
     * 修改密码成功
     */
    @Test
    @DisplayName("changePassword 修改密码成功")
    void shouldChangePasswordSuccessfully() {
        SysUser user = mockUser(USER_ID);
        user.setPassword("$2a$10$encodedOldPassword");
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("oldPass123", "$2a$10$encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass456")).thenReturn("$2a$10$encodedNewPassword");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("oldPass123");
        request.setNewPassword("newPass456");
        request.setConfirmPassword("newPass456");

        userProfileService.changePassword(USER_ID, request);

        verify(sysUserMapper).updateById(any(SysUser.class));
    }

    /**
     * 旧密码错误时抛出异常
     */
    @Test
    @DisplayName("changePassword 旧密码错误时抛出异常")
    void shouldThrowWhenOldPasswordIncorrect() {
        SysUser user = mockUser(USER_ID);
        user.setPassword("$2a$10$encodedOldPassword");
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("wrongPassword", "$2a$10$encodedOldPassword")).thenReturn(false);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPass456");
        request.setConfirmPassword("newPass456");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userProfileService.changePassword(USER_ID, request));

        assertEquals(ErrorCode.OLD_PASSWORD_ERROR.getCode(), exception.getCode());
    }

    /**
     * 两次新密码不一致时抛出异常
     */
    @Test
    @DisplayName("changePassword 两次新密码不一致时抛出异常")
    void shouldThrowWhenNewPasswordsNotMatch() {
        SysUser user = mockUser(USER_ID);
        user.setPassword("$2a$10$encodedOldPassword");
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("oldPass123", "$2a$10$encodedOldPassword")).thenReturn(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("oldPass123");
        request.setNewPassword("newPass456");
        request.setConfirmPassword("differentPass");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userProfileService.changePassword(USER_ID, request));

        assertEquals(ErrorCode.PASSWORD_NOT_MATCH.getCode(), exception.getCode());
    }

    /**
     * 学生概览统计成功
     */
    @Test
    @DisplayName("getStudentOverview 学生概览统计成功")
    void shouldReturnStudentOverviewWithCorrectStats() {
        SysUser user = mockUser(USER_ID);
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        mockUserRoles(USER_ID, List.of("STUDENT"));

        // 模拟订单数据
        when(errandOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                order(1L, 0),   // 待支付
                order(2L, 1),   // 待接单（进行中）
                order(3L, 7),   // 已完成
                order(4L, 8),   // 已取消
                order(5L, 10)   // 申诉中
        ));

        // 模拟未读消息
        when(stationMessageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        // 模拟跑腿员认证状态
        RunnerAuth auth = new RunnerAuth();
        auth.setAuthStatus(1);
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        UserCenterOverviewVO overview = userProfileService.getStudentOverview(USER_ID);

        assertNotNull(overview);
        assertEquals(5L, overview.getTotalOrderCount());
        assertEquals(1L, overview.getUnpaidOrderCount());
        assertEquals(1L, overview.getOngoingOrderCount());
        assertEquals(1L, overview.getCompletedOrderCount());
        assertEquals(1L, overview.getCancelledOrderCount());
        assertEquals(1L, overview.getAppealOrderCount());
        assertEquals(3L, overview.getUnreadMessageCount());
        assertEquals(1, overview.getRunnerAuthStatus());
    }

    /**
     * 跑腿员概览统计成功
     */
    @Test
    @DisplayName("getRunnerOverview 跑腿员概览统计成功")
    void shouldReturnRunnerOverviewWithCorrectStats() {
        SysUser user = mockUser(USER_ID);
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        mockUserRoles(USER_ID, List.of("RUNNER"));

        // 模拟认证信息
        RunnerAuth auth = new RunnerAuth();
        auth.setId(1L);
        auth.setUserId(USER_ID);
        auth.setAuthStatus(1);
        auth.setCurrentFlag(1);
        auth.setCertType(1);
        auth.setCertNo("123456789012345678");
        auth.setCreateTime(LocalDateTime.now());
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        // 模拟接单数据
        when(errandOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                order(1L, 2),   // 已接单（进行中）
                order(2L, 5),   // 配送中（进行中）
                order(3L, 7)    // 已完成
        ));

        // 模拟收益数据
        when(runnerIncomeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                income(1L, "10.00", 0),  // 待结算
                income(2L, "20.00", 2)   // 已结算
        ));

        // 模拟评价数据
        OrderEvaluation eval1 = new OrderEvaluation();
        eval1.setStarScore(5);
        OrderEvaluation eval2 = new OrderEvaluation();
        eval2.setStarScore(3);
        when(orderEvaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(eval1, eval2));

        RunnerCenterOverviewVO overview = userProfileService.getRunnerOverview(USER_ID);

        assertNotNull(overview);
        assertEquals(3L, overview.getAcceptedOrderCount());
        assertEquals(2L, overview.getOngoingOrderCount());
        assertEquals(1L, overview.getCompletedOrderCount());
        assertEquals(0, new BigDecimal("30.00").compareTo(overview.getTotalIncome()));
        assertEquals(0, new BigDecimal("10.00").compareTo(overview.getPendingIncome()));
        assertEquals(0, new BigDecimal("20.00").compareTo(overview.getSettledIncome()));
        assertEquals(0, new BigDecimal("4.0").compareTo(overview.getAverageScore()));
        assertEquals(2L, overview.getTotalEvaluationCount());
    }

    /**
     * 跑腿员认证信息脱敏成功
     */
    @Test
    @DisplayName("getRunnerAuthProfile 认证信息脱敏成功")
    void shouldReturnMaskedCertNo() {
        RunnerAuth auth = new RunnerAuth();
        auth.setId(1L);
        auth.setUserId(USER_ID);
        auth.setAuthStatus(1);
        auth.setCurrentFlag(1);
        auth.setCertType(1);
        auth.setCertNo("123456789012345678");
        auth.setCertFrontUrl("https://img.example.com/front.jpg");
        auth.setCertBackUrl("https://img.example.com/back.jpg");
        auth.setCreateTime(LocalDateTime.now());
        auth.setReviewTime(LocalDateTime.now());
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        RunnerAuthProfileVO result = userProfileService.getRunnerAuthProfile(USER_ID);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1, result.getAuthStatus());
        assertEquals(1, result.getCertType());
        // 证件号脱敏：前3后4，中间用****替换
        assertEquals("123****5678", result.getCertNoMasked());
        assertEquals("https://img.example.com/front.jpg", result.getCertFrontUrl());
        assertEquals("https://img.example.com/back.jpg", result.getCertBackUrl());
    }

    /**
     * 无认证记录时返回null
     */
    @Test
    @DisplayName("getRunnerAuthProfile 无认证记录时返回null")
    void shouldReturnNullWhenNoAuthRecord() {
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RunnerAuthProfileVO result = userProfileService.getRunnerAuthProfile(USER_ID);

        assertNull(result);
    }

    /**
     * 构造用户实体
     */
    private SysUser mockUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRealName("张三");
        user.setNickName("测试昵称");
        user.setPhone("13800138000");
        user.setAvatarUrl("https://img.example.com/avatar.jpg");
        user.setGender(1);
        user.setUserStatus(1);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    /**
     * Mock用户角色查询
     */
    private void mockUserRoles(Long userId, List<String> roleCodes) {
        List<SysUserMapper.UserRole> roles = roleCodes.stream().map(code -> {
            SysUserMapper.UserRole role = new SysUserMapper.UserRole();
            role.setId(1L);
            role.setRoleCode(code);
            return role;
        }).toList();
        when(sysUserMapper.selectUserRoles(userId)).thenReturn(roles);
    }

    /**
     * 构造订单实体
     */
    private ErrandOrder order(Long id, Integer status) {
        ErrandOrder order = new ErrandOrder();
        order.setId(id);
        order.setOrderStatus(status);
        return order;
    }

    /**
     * 构造收益记录实体
     */
    private RunnerIncomeRecord income(Long id, String amount, Integer settlementStatus) {
        RunnerIncomeRecord record = new RunnerIncomeRecord();
        record.setId(id);
        record.setRunnerId(USER_ID);
        record.setIncomeAmount(new BigDecimal(amount));
        record.setSettlementStatus(settlementStatus);
        return record;
    }
}
