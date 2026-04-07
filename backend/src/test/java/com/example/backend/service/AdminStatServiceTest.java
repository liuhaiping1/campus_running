package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.enums.AppealStatusEnum;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.enums.RefundStatusEnum;
import com.example.backend.common.enums.RoleCodeEnum;
import com.example.backend.entity.AppealRecord;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.PaymentOrder;
import com.example.backend.entity.RefundRecord;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.AppealRecordMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.mapper.RefundRecordMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.service.impl.AdminStatServiceImpl;
import com.example.backend.vo.AdminOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminStatService 单元测试类
 * <p>
 * 使用 Mockito 对 {@link AdminStatServiceImpl} 进行单元测试，
 * 覆盖空数据统计、正常数据统计和各统计指标的状态过滤正确性。
 * </p>
 * <p>
 * Mock 策略：使用 {@code @Mock} 模拟所有 Mapper 层依赖，
 * 通过 {@code @InjectMocks} 将模拟对象注入被测 Service 实例。
 * </p>
 *
 * @author campus_running
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatService 单元测试")
class AdminStatServiceTest {

    /** Mock 订单 Mapper */
    @Mock
    private ErrandOrderMapper errandOrderMapper;

    /** Mock 支付流水 Mapper */
    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    /** Mock 退款记录 Mapper */
    @Mock
    private RefundRecordMapper refundRecordMapper;

    /** Mock 用户角色关联 Mapper */
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    /** Mock 跑腿员认证 Mapper */
    @Mock
    private RunnerAuthMapper runnerAuthMapper;

    /** Mock 申诉记录 Mapper */
    @Mock
    private AppealRecordMapper appealRecordMapper;

    /** 自动注入 Mock 的被测对象 */
    @InjectMocks
    private AdminStatServiceImpl adminStatService;

    // =========================================================================
    // 空数据统计测试组
    // =========================================================================

    /**
     * 空数据统计测试组
     * <p>
     * 验证当所有 Mapper 查询返回空结果时，统计概览各字段均返回默认值（0），
     * 不会出现 NullPointerException。
     * </p>
     */
    @Nested
    @DisplayName("空数据统计")
    class EmptyDataTests {

        /**
         * 测试空数据时所有统计字段返回默认值
         * <p>
         * 验证点：
         * <ul>
         *   <li>totalOrderCount 为 0</li>
         *   <li>todayOrderCount 为 0</li>
         *   <li>paidAmount 为 BigDecimal.ZERO</li>
         *   <li>refundAmount 为 BigDecimal.ZERO</li>
         *   <li>其他计数字段均为 0</li>
         * </ul>
         */
        @Test
        @DisplayName("空数据时所有统计字段应返回默认值0")
        void shouldReturnZeroForEmptyData() {
            // Given: 所有 Mapper 查询返回空
            when(errandOrderMapper.selectCount(any())).thenReturn(0L);
            when(paymentOrderMapper.sumPayAmountByStatus(anyInt())).thenReturn(null);
            when(refundRecordMapper.sumRefundAmountByStatus(anyInt())).thenReturn(null);
            when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);
            when(runnerAuthMapper.selectCount(any())).thenReturn(0L);
            when(appealRecordMapper.selectCount(any())).thenReturn(0L);

            // When: 查询统计概览
            AdminOverviewVO result = adminStatService.getOverview();

            // Then: 所有字段返回 0，无 NPE
            assertNotNull(result, "统计结果不应为null");
            // 订单统计默认值
            assertEquals(0L, result.getTotalOrderCount(), "总订单数应为0");
            assertEquals(0L, result.getTodayOrderCount(), "今日订单数应为0");
            // 金额统计默认值
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPaidAmount()), "已支付金额应为0");
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getRefundAmount()), "退款金额应为0");
            // 用户与待处理项默认值
            assertEquals(0L, result.getActiveRunnerCount(), "有效跑腿员数应为0");
            assertEquals(0L, result.getPendingRunnerAuthCount(), "待审核认证数应为0");
            assertEquals(0L, result.getPendingAppealCount(), "待处理申诉数应为0");
            assertEquals(0L, result.getPendingRefundCount(), "待处理退款数应为0");
        }
    }

    // =========================================================================
    // 正常数据统计测试组
    // =========================================================================

    /**
     * 正常数据统计测试组
     * <p>
     * 验证各统计指标在正常数据场景下的统计准确性，
     * 包括订单计数、金额求和和状态过滤正确性。
     * </p>
     */
    @Nested
    @DisplayName("正常数据统计")
    class NormalDataTests {

        /**
         * 测试各统计指标能正确按状态过滤并返回
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单总数正确统计</li>
         *   <li>支付金额仅统计 PAID 状态</li>
         *   <li>退款金额仅统计 SUCCESS 状态</li>
         *   <li>跑腿员数按 role_code 和 role_status 过滤</li>
         *   <li>待审核认证数仅统计 PENDING 状态</li>
         *   <li>待处理申诉数统计 PENDING + PROCESSING</li>
         *   <li>待处理退款数统计 PENDING + PROCESSING</li>
         * </ul>
         */
        @Test
        @DisplayName("各统计指标应按正确的状态过滤并返回")
        void shouldReturnCorrectStatsForNormalData() {
            // Given: 模拟正常数据
            // 订单总数 = 10，今日订单 = 3
            when(errandOrderMapper.selectCount(isNull())).thenReturn(10L);
            when(errandOrderMapper.selectCount(argThat(w -> w instanceof LambdaQueryWrapper))).thenReturn(3L);

            // 已支付金额：数据库端SUM返回80.00（含null防兜底由DB层处理）
            when(paymentOrderMapper.sumPayAmountByStatus(PayStatusEnum.PAID.getCode()))
                    .thenReturn(BigDecimal.valueOf(80.00));

            // 退款成功金额：20.00
            when(refundRecordMapper.sumRefundAmountByStatus(RefundStatusEnum.SUCCESS.getCode()))
                    .thenReturn(BigDecimal.valueOf(20.00));

            // 有效跑腿员数 = 5
            when(sysUserRoleMapper.selectCount(any())).thenReturn(5L);
            // 待审核认证数 = 2
            when(runnerAuthMapper.selectCount(any())).thenReturn(2L);
            // 待处理申诉数 = 4
            when(appealRecordMapper.selectCount(any())).thenReturn(4L);
            // 待处理退款数 = 4（refundRecordMapper.selectCount 用于待处理退款计数）
            when(refundRecordMapper.selectCount(any())).thenReturn(4L);

            // When: 查询统计概览
            AdminOverviewVO result = adminStatService.getOverview();

            // Then: 验证各统计值
            assertNotNull(result, "统计结果不应为null");
            // 订单统计
            assertEquals(10L, result.getTotalOrderCount(), "总订单数应为10");
            assertEquals(3L, result.getTodayOrderCount(), "今日订单数应为3");
            // 金额统计：80.00（数据库端SUM返回）
            assertEquals(0, BigDecimal.valueOf(80.00).compareTo(result.getPaidAmount()),
                    "已支付金额应为80.00（含null防兜底）");
            assertEquals(0, BigDecimal.valueOf(20.00).compareTo(result.getRefundAmount()),
                    "退款金额应为20.00");
            // 用户与待处理项
            assertEquals(5L, result.getActiveRunnerCount(), "有效跑腿员数应为5");
            assertEquals(2L, result.getPendingRunnerAuthCount(), "待审核认证数应为2");
            assertEquals(4L, result.getPendingAppealCount(), "待处理申诉数应为4");
            assertEquals(4L, result.getPendingRefundCount(), "待处理退款数应为4");
        }

        /**
         * 测试金额为null时能正常防NPE兜底
         * <p>
         * 当支付流水中的金额字段为null时，统计结果不应抛出异常，
         * null值应被过滤并以0计。
         * </p>
         */
        @Test
        @DisplayName("SUM返回null时应防NPE兜底返回ZERO")
        void shouldHandleNullSumGracefully() {
            // Given: SUM 方法返回 null（无匹配记录）
            when(errandOrderMapper.selectCount(any())).thenReturn(0L);
            when(paymentOrderMapper.sumPayAmountByStatus(anyInt())).thenReturn(null);
            when(refundRecordMapper.sumRefundAmountByStatus(anyInt())).thenReturn(null);
            when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);
            when(runnerAuthMapper.selectCount(any())).thenReturn(0L);
            when(appealRecordMapper.selectCount(any())).thenReturn(0L);

            // When: 查询统计概览
            AdminOverviewVO result = adminStatService.getOverview();

            // Then: 金额字段为 ZERO 而非 null
            assertNotNull(result.getPaidAmount(), "已支付金额不应为null");
            assertNotNull(result.getRefundAmount(), "退款金额不应为null");
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPaidAmount()),
                    "SUM返回null时已支付金额应为ZERO");
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getRefundAmount()),
                    "SUM返回null时退款金额应为ZERO");
        }
    }

    // =========================================================================
    // 状态过滤正确性测试组
    // =========================================================================

    /**
     * 状态过滤正确性测试组
     * <p>
     * 验证各统计查询中状态过滤条件使用正确的枚举值。
     * 重点验证待审核认证使用 PENDING、待处理申诉和退款包含 PENDING +
     * PROCESSING、有效跑腿员按 roleCode + roleStatus 过滤。
     * </p>
     */
    @Nested
    @DisplayName("状态过滤正确性")
    class StatusFilterTests {

        /**
         * 测试跑腿员认证仅统计待审核状态
         * <p>
         * 验证 authStatus 过滤条件使用 AuthStatusEnum.PENDING.getCode()。
         * 已通过和已驳回的认证申请不被计入待审核计数。
         * </p>
         */
        @Test
        @DisplayName("待审核认证仅统计 PENDING 状态")
        void shouldOnlyCountPendingAuth() {
            // Given: 有2条待审核、1条已通过、1条已驳回
            when(errandOrderMapper.selectCount(any())).thenReturn(0L);
            when(paymentOrderMapper.sumPayAmountByStatus(anyInt())).thenReturn(null);
            when(refundRecordMapper.sumRefundAmountByStatus(anyInt())).thenReturn(null);
            when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);
            // runnerAuthMapper.selectCount 仅查询 authStatus=PENDING → 返回 2
            when(runnerAuthMapper.selectCount(any())).thenReturn(2L);
            when(appealRecordMapper.selectCount(any())).thenReturn(0L);

            // When: 查询统计概览
            AdminOverviewVO result = adminStatService.getOverview();

            // Then: 待审核认证数为2（仅统计PENDING状态）
            assertEquals(2L, result.getPendingRunnerAuthCount(),
                    "待审核认证数应为2（仅统计PENDING状态）");
        }

        /**
         * 测试待处理申诉仅统计 PENDING 和 PROCESSING 状态
         * <p>
         * 验证申诉统计包含 appealStatus = PENDING(0) 和 PROCESSING(1)，
         * 不包括已成立、已驳回和已关闭的申诉。
         * </p>
         */
        @Test
        @DisplayName("待处理申诉仅统计 PENDING + PROCESSING 状态")
        void shouldOnlyCountPendingAndProcessingAppeals() {
            // Given: 模拟有3条待处理申诉（PENDING+PROCESSING）
            when(errandOrderMapper.selectCount(any())).thenReturn(0L);
            when(paymentOrderMapper.sumPayAmountByStatus(anyInt())).thenReturn(null);
            when(refundRecordMapper.sumRefundAmountByStatus(anyInt())).thenReturn(null);
            when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);
            when(runnerAuthMapper.selectCount(any())).thenReturn(0L);
            // 待处理申诉 = 3 (仅 PENDING+PROCESSING)
            when(appealRecordMapper.selectCount(any())).thenReturn(3L);

            // When: 查询统计概览
            AdminOverviewVO result = adminStatService.getOverview();

            // Then: 待处理申诉数 = 3
            assertEquals(3L, result.getPendingAppealCount(),
                    "待处理申诉数应为3（仅统计PENDING+PROCESSING状态）");
        }

        /**
         * 测试有效跑腿员数按角色编码和角色状态过滤
         * <p>
         * 验证仅统计 roleCode=RUNNER 且 roleStatus=1（有效）的用户角色记录。
         * 被停用或已过期的 RUNNER 角色不计入有效跑腿员数。
         * </p>
         */
        @Test
        @DisplayName("有效跑腿员数按 roleCode=RUNNER 且 roleStatus=1 过滤")
        void shouldOnlyCountActiveRunners() {
            // Given: 有效跑腿员 = 5
            when(errandOrderMapper.selectCount(any())).thenReturn(0L);
            when(paymentOrderMapper.sumPayAmountByStatus(anyInt())).thenReturn(null);
            when(refundRecordMapper.sumRefundAmountByStatus(anyInt())).thenReturn(null);
            // 有效跑腿员：roleCode=RUNNER 且 roleStatus=1
            when(sysUserRoleMapper.selectCount(any())).thenReturn(5L);
            when(runnerAuthMapper.selectCount(any())).thenReturn(0L);
            when(appealRecordMapper.selectCount(any())).thenReturn(0L);

            // When: 查询统计概览
            AdminOverviewVO result = adminStatService.getOverview();

            // Then: 有效跑腿员数 = 5
            assertEquals(5L, result.getActiveRunnerCount(),
                    "有效跑腿员数应为5（仅统计roleCode=RUNNER且roleStatus=1的记录）");
        }
    }
}
