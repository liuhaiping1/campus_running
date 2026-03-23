package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.RunnerAuthApplyRequest;
import com.example.backend.dto.request.RunnerAuthReviewRequest;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.SysRole;
import com.example.backend.entity.SysUser;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.SysRoleMapper;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.service.impl.RunnerAuthServiceImpl;
import com.example.backend.vo.RunnerAuthVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RunnerAuthService 单元测试类。
 * <p>
 * 使用 Mockito 对 {@link RunnerAuthServiceImpl} 进行单元测试，
 * 覆盖认证申请、审核和列表查询三个核心流程，共计 10 个测试用例。
 * 不启动 Spring 容器，所有依赖均通过 Mock 注入。
 * </p>
 *
 * @see RunnerAuthServiceImpl
 * @see RunnerAuthService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RunnerAuthService 单元测试")
class RunnerAuthServiceTest {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RunnerAuth.class);
    }

    /**
     * 跑腿员认证 Mapper 的 Mock 对象。
     */
    @Mock
    private RunnerAuthMapper runnerAuthMapper;

    /**
     * 系统用户 Mapper 的 Mock 对象。
     */
    @Mock
    private SysUserMapper sysUserMapper;

    /**
     * 系统角色 Mapper 的 Mock 对象。
     */
    @Mock
    private SysRoleMapper sysRoleMapper;

    /**
     * 用户角色关联 Mapper 的 Mock 对象。
     */
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 被测试的 RunnerAuthService 实现类实例，由 Mockito 自动注入上述 Mock 依赖。
     */
    @InjectMocks
    private RunnerAuthServiceImpl runnerAuthService;

    // ==================== 认证申请测试组 ====================

    /**
     * 认证申请相关的测试用例。
     * <p>覆盖正常提交、待审核冲突、已通过冲突三种场景。</p>
     */
    @Nested
    @DisplayName("认证申请")
    class ApplyTests {

        /**
         * 测试正常场景：用户无待审记录也无已通过记录时，成功提交认证申请。
         * <p>验证：</p>
         * <ul>
         *   <li>旧记录的 currentFlag 被标记为 0（历史记录）</li>
         *   <li>新记录被插入，且字段值正确</li>
         *   <li>authStatus 为 PENDING，currentFlag 为 1</li>
         *   <li>authBatchNo 自动生成且非空</li>
         * </ul>
         */
        @Test
        @DisplayName("无待审/已通过记录时，应成功提交认证申请")
        void shouldApplySuccessfully() {
            RunnerAuthApplyRequest request = new RunnerAuthApplyRequest();
            request.setSchoolName("清华大学");
            request.setCampusName("海淀校区");
            request.setCertType(1);
            request.setCertNo("2021001");
            request.setCertFrontUrl("http://example.com/front.jpg");
            request.setCertBackUrl("http://example.com/back.jpg");

            when(runnerAuthMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(runnerAuthMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(runnerAuthMapper.insert(any(RunnerAuth.class))).thenReturn(1);

            runnerAuthService.apply(1L, request);

            // 验证：旧记录被标记为历史
            verify(runnerAuthMapper).update(isNull(), any(LambdaUpdateWrapper.class));

            // 捕获插入的认证记录并验证字段
            ArgumentCaptor<RunnerAuth> captor = ArgumentCaptor.forClass(RunnerAuth.class);
            verify(runnerAuthMapper).insert(captor.capture());
            RunnerAuth saved = captor.getValue();

            assertEquals(1L, saved.getUserId(), "userId 应为请求传入值");
            assertNotNull(saved.getAuthBatchNo(), "authBatchNo 应自动生成");
            assertFalse(saved.getAuthBatchNo().isEmpty(), "authBatchNo 不应为空字符串");
            assertEquals("清华大学", saved.getSchoolName(), "schoolName 应与请求一致");
            assertEquals("海淀校区", saved.getCampusName(), "campusName 应与请求一致");
            assertEquals(1, saved.getCertType(), "certType 应与请求一致");
            assertEquals("2021001", saved.getCertNo(), "certNo 应与请求一致");
            assertEquals("http://example.com/front.jpg", saved.getCertFrontUrl(),
                    "certFrontUrl 应与请求一致");
            assertEquals("http://example.com/back.jpg", saved.getCertBackUrl(),
                    "certBackUrl 应与请求一致");
            assertEquals(AuthStatusEnum.PENDING.getCode(), saved.getAuthStatus(),
                    "新记录状态应为待审核");
            assertEquals(1, saved.getCurrentFlag(), "新记录 currentFlag 应为 1");
        }

        /**
         * 测试冲突场景：用户已存在待审核的认证申请时，抛出 AUTH_PENDING_EXISTS 异常。
         * <p>验证：</p>
         * <ul>
         *   <li>抛出 {@link BusinessException}，错误码为 AUTH_PENDING_EXISTS</li>
         *   <li>不执行 update 和 insert 操作</li>
         * </ul>
         */
        @Test
        @DisplayName("存在待审核记录时，应抛出 AUTH_PENDING_EXISTS 异常")
        void shouldThrowWhenPendingExists() {
            RunnerAuthApplyRequest request = new RunnerAuthApplyRequest();
            request.setSchoolName("北京大学");
            request.setCertType(1);
            request.setCertFrontUrl("http://example.com/front.jpg");

            when(runnerAuthMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> runnerAuthService.apply(1L, request));
            assertEquals(ErrorCode.AUTH_PENDING_EXISTS.getCode(), ex.getCode(),
                    "错误码应为 AUTH_PENDING_EXISTS");

            verify(runnerAuthMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(runnerAuthMapper, never()).insert(any(RunnerAuth.class));
        }

        /**
         * 测试冲突场景：用户无待审记录但已通过认证时，抛出 AUTH_ALREADY_APPROVED 异常。
         * <p>验证：</p>
         * <ul>
         *   <li>第一次 selectCount（待审检查）返回 0，第二次（已通过检查）返回 1</li>
         *   <li>抛出 {@link BusinessException}，错误码为 AUTH_ALREADY_APPROVED</li>
         *   <li>不执行 update 和 insert 操作</li>
         * </ul>
         */
        @Test
        @DisplayName("已通过认证时，应抛出 AUTH_ALREADY_APPROVED 异常")
        void shouldThrowWhenAlreadyApproved() {
            RunnerAuthApplyRequest request = new RunnerAuthApplyRequest();
            request.setSchoolName("浙江大学");
            request.setCertType(2);
            request.setCertFrontUrl("http://example.com/card.jpg");

            when(runnerAuthMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L, 1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> runnerAuthService.apply(1L, request));
            assertEquals(ErrorCode.AUTH_ALREADY_APPROVED.getCode(), ex.getCode(),
                    "错误码应为 AUTH_ALREADY_APPROVED");

            verify(runnerAuthMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(runnerAuthMapper, never()).insert(any(RunnerAuth.class));
        }
    }

    // ==================== 认证审核测试组 ====================

    /**
     * 认证审核相关的测试用例。
     * <p>覆盖审核通过（含角色授予去重）、驳回、记录不存在和重复审核五种场景。</p>
     */
    @Nested
    @DisplayName("认证审核")
    class ReviewTests {

        /**
         * 测试审核通过场景：待审认证被管理员审核通过，且用户尚无 RUNNER 角色时自动授予。
         * <p>验证：</p>
         * <ul>
         *   <li>认证记录状态更新为 APPROVED，审核管理员和审核时间被正确设置</li>
         *   <li>系统查询 RUNNER 角色并创建 SysUserRole 关联</li>
         *   <li>SysUserRole 的 grantSource=2（认证通过）、roleCode="RUNNER"</li>
         *   <li>调用 updateById 持久化认证记录</li>
         * </ul>
         */
        @Test
        @DisplayName("审核通过且用户无 RUNNER 角色时，应授予角色")
        void shouldApproveAndGrantRunnerRole() {
            Long authId = 1L;
            Long adminId = 200L;
            Long userId = 100L;

            RunnerAuth auth = new RunnerAuth();
            auth.setId(authId);
            auth.setUserId(userId);
            auth.setAuthStatus(AuthStatusEnum.PENDING.getCode());

            RunnerAuthReviewRequest request = new RunnerAuthReviewRequest();
            request.setAuthStatus(AuthStatusEnum.APPROVED.getCode());

            SysRole runnerRole = new SysRole();
            runnerRole.setId(2L);
            runnerRole.setRoleCode("RUNNER");
            runnerRole.setRoleStatus(1);

            when(runnerAuthMapper.selectById(authId)).thenReturn(auth);
            when(sysUserRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysRoleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runnerRole);
            when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);
            when(runnerAuthMapper.updateById(any(RunnerAuth.class))).thenReturn(1);

            runnerAuthService.review(authId, adminId, request);

            // 验证认证记录状态更新
            assertEquals(AuthStatusEnum.APPROVED.getCode(), auth.getAuthStatus(),
                    "认证状态应更新为已通过");
            assertEquals(adminId, auth.getReviewAdminId(), "审核管理员应正确设置");
            assertNotNull(auth.getReviewTime(), "审核时间应已设置");

            // 验证 RUNNER 角色被授予
            ArgumentCaptor<SysUserRole> roleCaptor = ArgumentCaptor.forClass(SysUserRole.class);
            verify(sysUserRoleMapper).insert(roleCaptor.capture());
            SysUserRole savedUserRole = roleCaptor.getValue();
            assertEquals(userId, savedUserRole.getUserId(), "userRole 的 userId 应一致");
            assertEquals(2L, savedUserRole.getRoleId(), "userRole 的 roleId 应为 RUNNER 角色 ID");
            assertEquals("RUNNER", savedUserRole.getRoleCode(), "roleCode 应为 RUNNER");
            assertEquals(2, savedUserRole.getGrantSource(), "grantSource 应为 2（认证通过）");
            assertEquals(1, savedUserRole.getRoleStatus(), "roleStatus 应为 1（有效）");
            assertNotNull(savedUserRole.getGrantTime(), "grantTime 应已设置");

            verify(runnerAuthMapper).updateById(auth);
        }

        /**
         * 测试审核通过但角色已存在场景：用户已经拥有 RUNNER 角色时，不重复授予。
         * <p>验证：</p>
         * <ul>
         *   <li>认证记录状态仍更新为 APPROVED</li>
         *   <li>不创建新的 SysUserRole（无 insert 调用）</li>
         *   <li>不查询 SysRole（无 selectOne 调用）</li>
         * </ul>
         */
        @Test
        @DisplayName("审核通过但用户已有 RUNNER 角色时，不应重复授予")
        void shouldApproveWhenRunnerRoleAlreadyExists() {
            Long authId = 1L;
            Long adminId = 200L;

            RunnerAuth auth = new RunnerAuth();
            auth.setId(authId);
            auth.setUserId(100L);
            auth.setAuthStatus(AuthStatusEnum.PENDING.getCode());

            RunnerAuthReviewRequest request = new RunnerAuthReviewRequest();
            request.setAuthStatus(AuthStatusEnum.APPROVED.getCode());

            when(runnerAuthMapper.selectById(authId)).thenReturn(auth);
            when(sysUserRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(runnerAuthMapper.updateById(any(RunnerAuth.class))).thenReturn(1);

            runnerAuthService.review(authId, adminId, request);

            assertEquals(AuthStatusEnum.APPROVED.getCode(), auth.getAuthStatus(),
                    "认证状态应更新为已通过");

            // 验证角色相关操作均未被调用
            verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
            verify(sysRoleMapper, never()).selectOne(any(LambdaQueryWrapper.class));

            verify(runnerAuthMapper).updateById(auth);
        }

        /**
         * 测试驳回场景：待审认证被驳回时，设置驳回原因，不授予角色。
         * <p>验证：</p>
         * <ul>
         *   <li>认证状态更新为 REJECTED，驳回原因被正确设置</li>
         *   <li>审核管理员和审核时间被设置</li>
         *   <li>所有角色相关操作均未被调用（不触发 grantRunnerRoleIfAbsent）</li>
         * </ul>
         */
        @Test
        @DisplayName("驳回时应设置驳回原因，且不授予角色")
        void shouldRejectWithReason() {
            Long authId = 1L;
            Long adminId = 200L;
            String rejectReason = "证件照片不清晰，请重新上传";

            RunnerAuth auth = new RunnerAuth();
            auth.setId(authId);
            auth.setUserId(100L);
            auth.setAuthStatus(AuthStatusEnum.PENDING.getCode());

            RunnerAuthReviewRequest request = new RunnerAuthReviewRequest();
            request.setAuthStatus(AuthStatusEnum.REJECTED.getCode());
            request.setRejectReason(rejectReason);

            when(runnerAuthMapper.selectById(authId)).thenReturn(auth);
            when(runnerAuthMapper.updateById(any(RunnerAuth.class))).thenReturn(1);

            runnerAuthService.review(authId, adminId, request);

            assertEquals(AuthStatusEnum.REJECTED.getCode(), auth.getAuthStatus(),
                    "认证状态应更新为已驳回");
            assertEquals(rejectReason, auth.getRejectReason(), "驳回原因应正确设置");
            assertEquals(adminId, auth.getReviewAdminId(), "审核管理员应正确设置");
            assertNotNull(auth.getReviewTime(), "审核时间应已设置");

            // 验证角色操作未被触发
            verify(sysUserRoleMapper, never()).selectCount(any(LambdaQueryWrapper.class));
            verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
            verify(sysRoleMapper, never()).selectOne(any(LambdaQueryWrapper.class));

            verify(runnerAuthMapper).updateById(auth);
        }

        /**
         * 测试认证记录不存在场景：根据 ID 查询不到记录时，抛出 AUTH_NOT_FOUND 异常。
         * <p>验证：</p>
         * <ul>
         *   <li>selectById 返回 null</li>
         *   <li>抛出 {@link BusinessException}，错误码为 AUTH_NOT_FOUND</li>
         *   <li>不执行 updateById 和角色相关操作</li>
         * </ul>
         */
        @Test
        @DisplayName("认证记录不存在时，应抛出 AUTH_NOT_FOUND 异常")
        void shouldThrowWhenAuthNotFound() {
            RunnerAuthReviewRequest request = new RunnerAuthReviewRequest();
            request.setAuthStatus(AuthStatusEnum.APPROVED.getCode());

            when(runnerAuthMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> runnerAuthService.review(1L, 200L, request));
            assertEquals(ErrorCode.AUTH_NOT_FOUND.getCode(), ex.getCode(),
                    "错误码应为 AUTH_NOT_FOUND");

            verify(runnerAuthMapper, never()).updateById(any(RunnerAuth.class));
            verify(sysUserRoleMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        }

        /**
         * 测试重复审核场景：认证记录已被审核（非 PENDING 状态），再次审核时抛出 CONFLICT 异常。
         * <p>验证：</p>
         * <ul>
         *   <li>认证记录的 authStatus 为 APPROVED（已审核通过）</li>
         *   <li>抛出 {@link BusinessException}，错误码为 CONFLICT</li>
         *   <li>异常信息包含"已审核"关键字</li>
         *   <li>不执行 updateById 和角色相关操作</li>
         * </ul>
         */
        @Test
        @DisplayName("已审核的认证再次审核时，应抛出 CONFLICT 异常")
        void shouldThrowWhenAlreadyReviewed() {
            RunnerAuth auth = new RunnerAuth();
            auth.setId(1L);
            auth.setUserId(100L);
            auth.setAuthStatus(AuthStatusEnum.APPROVED.getCode());

            RunnerAuthReviewRequest request = new RunnerAuthReviewRequest();
            request.setAuthStatus(AuthStatusEnum.APPROVED.getCode());

            when(runnerAuthMapper.selectById(1L)).thenReturn(auth);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> runnerAuthService.review(1L, 200L, request));
            assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode(),
                    "错误码应为 CONFLICT");
            assertTrue(ex.getMessage().contains("已审核"),
                    "异常信息应包含'已审核'");

            verify(runnerAuthMapper, never()).updateById(any(RunnerAuth.class));
            verify(sysUserRoleMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        }
    }

    // ==================== 认证列表查询测试组 ====================

    /**
     * 认证列表查询相关的测试用例。
     * <p>覆盖分页查询结果组装和按状态筛选两种场景。</p>
     */
    @Nested
    @DisplayName("认证列表查询")
    class ListTests {

        /**
         * 测试分页查询正常返回：查询结果包含用户真实姓名和手机号。
         * <p>验证：</p>
         * <ul>
         *   <li>分页信息（total、current、size）被正确保留</li>
         *   <li>通过 selectBatchIds 批量查询用户，将 realName 和 phone 填充到 VO</li>
         *   <li>返回列表的元素顺序与原始数据一致</li>
         * </ul>
         */
        @Test
        @DisplayName("分页查询应返回包含用户 realName 和 phone 的 VO 列表")
        void shouldReturnPaginatedList() {
            RunnerAuth auth1 = new RunnerAuth();
            auth1.setId(1L);
            auth1.setUserId(10L);
            auth1.setAuthStatus(AuthStatusEnum.PENDING.getCode());
            auth1.setSchoolName("清华大学");

            RunnerAuth auth2 = new RunnerAuth();
            auth2.setId(2L);
            auth2.setUserId(20L);
            auth2.setAuthStatus(AuthStatusEnum.APPROVED.getCode());
            auth2.setSchoolName("北京大学");

            Page<RunnerAuth> authPage = new Page<>(1, 10, 2);
            authPage.setRecords(List.of(auth1, auth2));

            SysUser user1 = new SysUser();
            user1.setId(10L);
            user1.setRealName("张三");
            user1.setPhone("13800138000");

            SysUser user2 = new SysUser();
            user2.setId(20L);
            user2.setRealName("李四");
            user2.setPhone("13900139000");

            when(runnerAuthMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(authPage);
            when(sysUserMapper.selectBatchIds(anyList())).thenReturn(List.of(user1, user2));

            IPage<RunnerAuthVO> result = runnerAuthService.list(1, 10, null);

            // 验证分页信息
            assertEquals(2, result.getTotal(), "总记录数应为 2");
            assertEquals(1, result.getCurrent(), "当前页码应为 1");
            assertEquals(10, result.getSize(), "每页大小应为 10");

            // 验证 VO 列表
            List<RunnerAuthVO> records = result.getRecords();
            assertEquals(2, records.size(), "记录数应为 2");

            RunnerAuthVO vo1 = records.get(0);
            assertEquals(1L, vo1.getId(), "第一条记录 ID 应为 1");
            assertEquals("张三", vo1.getRealName(), "第一条记录的 realName 应为张三");
            assertEquals("13800138000", vo1.getPhone(), "第一条记录的 phone 应正确");
            assertEquals("清华大学", vo1.getSchoolName(), "第一条记录的 schoolName 应正确");
            assertEquals(AuthStatusEnum.PENDING.getCode(), vo1.getAuthStatus(),
                    "第一条记录的 authStatus 应为待审");

            RunnerAuthVO vo2 = records.get(1);
            assertEquals(2L, vo2.getId(), "第二条记录 ID 应为 2");
            assertEquals("李四", vo2.getRealName(), "第二条记录的 realName 应为李四");
            assertEquals("13900139000", vo2.getPhone(), "第二条记录的 phone 应正确");
            assertEquals("北京大学", vo2.getSchoolName(), "第二条记录的 schoolName 应正确");
            assertEquals(AuthStatusEnum.APPROVED.getCode(), vo2.getAuthStatus(),
                    "第二条记录的 authStatus 应为已通过");

            verify(runnerAuthMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
            verify(sysUserMapper).selectBatchIds(anyList());
        }

        /**
         * 测试按认证状态筛选：传入 authStatus 参数时，LambdaQueryWrapper 应包含对应条件。
         * <p>验证：</p>
         * <ul>
         *   <li>selectPage 的第二个参数（LambdaQueryWrapper）的 SQL 片段中包含 auth_status 列筛选条件</li>
         *   <li>筛选后的查询能正常返回</li>
         * </ul>
         */
        @Test
        @DisplayName("传入 authStatus 时，LambdaQueryWrapper 应包含认证状态筛选条件")
        void shouldFilterByAuthStatus() {
            Page<RunnerAuth> authPage = new Page<>(1, 10, 0);
            authPage.setRecords(List.of());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<LambdaQueryWrapper<RunnerAuth>> wrapperCaptor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            when(runnerAuthMapper.selectPage(any(Page.class), wrapperCaptor.capture()))
                    .thenReturn(authPage);

            runnerAuthService.list(1, 10, AuthStatusEnum.PENDING.getCode());

            LambdaQueryWrapper<RunnerAuth> capturedWrapper = wrapperCaptor.getValue();
            assertNotNull(capturedWrapper, "应成功捕获 LambdaQueryWrapper");

            String sqlSegment = capturedWrapper.getSqlSegment();
            assertNotNull(sqlSegment, "SQL Segment 不应为 null");
            assertTrue(sqlSegment.contains("auth_status"),
                    "LambdaQueryWrapper 的 SQL 片段应包含 auth_status 筛选条件，"
                            + "实际 SQL: " + sqlSegment);
        }
    }
}
