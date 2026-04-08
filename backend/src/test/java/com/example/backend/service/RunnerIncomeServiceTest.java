package com.example.backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.SettlementStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.RunnerIncomeRecord;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.RunnerIncomeRecordMapper;
import com.example.backend.service.impl.RunnerIncomeServiceImpl;
import com.example.backend.vo.RunnerIncomeOverviewVO;
import com.example.backend.vo.RunnerIncomeRecordVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 跑腿员收益服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RunnerIncomeService 单元测试")
class RunnerIncomeServiceTest {

    /**
     * 测试用户ID。
     */
    private static final Long USER_ID = 100L;

    /**
     * 跑腿员收益记录 Mapper。
     */
    @Mock
    private RunnerIncomeRecordMapper runnerIncomeRecordMapper;

    /**
     * 跑腿员认证 Mapper。
     */
    @Mock
    private RunnerAuthMapper runnerAuthMapper;

    /**
     * 被测收益服务。
     */
    @InjectMocks
    private RunnerIncomeServiceImpl runnerIncomeService;

    /**
     * 初始化 MyBatis-Plus 表元信息，便于单元测试断言 LambdaWrapper SQL 片段。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, RunnerIncomeRecord.class);
        TableInfoHelper.initTableInfo(assistant, RunnerAuth.class);
    }

    /**
     * 空收益记录时应返回零金额和零数量。
     */
    @Test
    @DisplayName("overview 空记录时返回零值")
    void shouldReturnZeroOverviewWhenNoRecord() {
        mockApprovedRunner(USER_ID);
        when(runnerIncomeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        RunnerIncomeOverviewVO overview = runnerIncomeService.overview(USER_ID);

        assertEquals(0, BigDecimal.ZERO.compareTo(overview.getTotalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(overview.getPendingIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(overview.getSettledIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(overview.getSettlingIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(overview.getRolledBackIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(overview.getCurrentMonthIncome()));
        assertEquals(0L, overview.getTotalOrderCount());
    }

    /**
     * 多种结算状态应分别汇总到对应字段。
     */
    @Test
    @DisplayName("overview 汇总多状态收益")
    void shouldSummarizeOverviewBySettlementStatus() {
        mockApprovedRunner(USER_ID);
        LocalDateTime now = LocalDateTime.now();
        when(runnerIncomeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                income(1L, USER_ID, "10.00", "1.00", SettlementStatusEnum.PENDING.getCode(), null, now),
                income(2L, USER_ID, "20.00", "2.00", SettlementStatusEnum.SETTLING.getCode(), null, now),
                income(3L, USER_ID, "30.00", "3.00", SettlementStatusEnum.SETTLED.getCode(), null, now),
                income(4L, USER_ID, "40.00", "4.00", SettlementStatusEnum.ROLLED_BACK.getCode(), "5.00", now)
        ));

        RunnerIncomeOverviewVO overview = runnerIncomeService.overview(USER_ID);

        assertEquals(0, new BigDecimal("100.00").compareTo(overview.getTotalIncome()));
        assertEquals(0, new BigDecimal("10.00").compareTo(overview.getPendingIncome()));
        assertEquals(0, new BigDecimal("20.00").compareTo(overview.getSettlingIncome()));
        assertEquals(0, new BigDecimal("30.00").compareTo(overview.getSettledIncome()));
        assertEquals(0, new BigDecimal("5.00").compareTo(overview.getRolledBackIncome()));
        assertEquals(4L, overview.getTotalOrderCount());
    }

    /**
     * 本月收益只统计 createTime 位于当前月份内的记录。
     */
    @Test
    @DisplayName("overview 只统计本月收益")
    void shouldOnlyCountCurrentMonthIncome() {
        mockApprovedRunner(USER_ID);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMonth = now.minusMonths(1);
        when(runnerIncomeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                income(1L, USER_ID, "12.50", null, SettlementStatusEnum.PENDING.getCode(), null, now),
                income(2L, USER_ID, "99.00", null, SettlementStatusEnum.SETTLED.getCode(), null, lastMonth)
        ));

        RunnerIncomeOverviewVO overview = runnerIncomeService.overview(USER_ID);

        assertEquals(0, new BigDecimal("111.50").compareTo(overview.getTotalIncome()));
        assertEquals(0, new BigDecimal("12.50").compareTo(overview.getCurrentMonthIncome()));
    }

    /**
     * 收益明细列表应按状态筛选并映射为 VO 分页结果。
     */
    @Test
    @DisplayName("list 成功返回并支持结算状态筛选")
    void shouldListIncomeRecordsWithSettlementStatusFilter() {
        mockApprovedRunner(USER_ID);
        RunnerIncomeRecord record = income(1L, USER_ID, "18.00", "2.00",
                SettlementStatusEnum.SETTLED.getCode(), null, LocalDateTime.now());
        record.setOrderId(88L);
        record.setSettlementTime(LocalDateTime.now());

        Page<RunnerIncomeRecord> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(record));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RunnerIncomeRecord>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(runnerIncomeRecordMapper.selectPage(any(Page.class), wrapperCaptor.capture())).thenReturn(page);

        IPage<RunnerIncomeRecordVO> result = runnerIncomeService.list(USER_ID, 1, 10,
                SettlementStatusEnum.SETTLED.getCode());

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        RunnerIncomeRecordVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals(88L, vo.getOrderId());
        assertEquals(USER_ID, vo.getRunnerId());
        assertEquals(SettlementStatusEnum.SETTLED.getCode(), vo.getSettlementStatus());

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("settlement_status"));
        assertTrue(sqlSegment.contains("create_time"));
    }

    /**
     * 收益明细列表必须只查询当前登录用户对应的 runnerId。
     */
    @Test
    @DisplayName("list 只查询当前 runnerId")
    void shouldOnlyQueryCurrentRunnerId() {
        mockApprovedRunner(USER_ID);
        Page<RunnerIncomeRecord> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RunnerIncomeRecord>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(runnerIncomeRecordMapper.selectPage(any(Page.class), wrapperCaptor.capture())).thenReturn(page);

        runnerIncomeService.list(USER_ID, 1, 10, null);

        LambdaQueryWrapper<RunnerIncomeRecord> wrapper = wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("runner_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(USER_ID));
        assertFalse(wrapper.getParamNameValuePairs().containsValue(999L));
    }

    /**
     * 非法分页参数应被拒绝。
     */
    @Test
    @DisplayName("list 非法分页参数失败")
    void shouldThrowWhenPageInvalid() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> runnerIncomeService.list(USER_ID, 0, 10, null),
                "页码小于1时应抛出业务异常");

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
    }

    /**
     * 非法结算状态应被拒绝。
     */
    @Test
    @DisplayName("list 非法结算状态失败")
    void shouldThrowWhenSettlementStatusInvalid() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> runnerIncomeService.list(USER_ID, 1, 10, 99),
                "结算状态非法时应抛出业务异常");

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
    }

    /**
     * 未认证跑腿员不能查询收益。
     */
    @Test
    @DisplayName("未认证跑腿员查询收益失败")
    void shouldThrowWhenRunnerNotApproved() {
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> runnerIncomeService.overview(USER_ID),
                "未认证跑腿员查询收益时应抛出业务异常");

        assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode());
    }

    /**
     * Mock 当前用户为已认证跑腿员。
     *
     * @param userId 用户ID
     */
    private void mockApprovedRunner(Long userId) {
        RunnerAuth auth = new RunnerAuth();
        auth.setId(7L);
        auth.setUserId(userId);
        auth.setAuthStatus(AuthStatusEnum.APPROVED.getCode());
        auth.setCurrentFlag(1);
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);
    }

    /**
     * 构造收益记录。
     *
     * @param id               记录ID
     * @param runnerId         跑腿员用户ID
     * @param incomeAmount     收益金额
     * @param commissionAmount 平台抽佣金额
     * @param status           结算状态
     * @param rollbackAmount   回滚金额
     * @param createTime       创建时间
     * @return 收益记录
     */
    private RunnerIncomeRecord income(Long id, Long runnerId, String incomeAmount, String commissionAmount,
                                      Integer status, String rollbackAmount, LocalDateTime createTime) {
        RunnerIncomeRecord record = new RunnerIncomeRecord();
        record.setId(id);
        record.setOrderId(id + 1000);
        record.setRunnerId(runnerId);
        record.setIncomeAmount(new BigDecimal(incomeAmount));
        record.setCommissionAmount(commissionAmount == null ? null : new BigDecimal(commissionAmount));
        record.setSettlementStatus(status);
        record.setRollbackAmount(rollbackAmount == null ? null : new BigDecimal(rollbackAmount));
        record.setCreateTime(createTime);
        record.setIsDeleted(0);
        return record;
    }
}
