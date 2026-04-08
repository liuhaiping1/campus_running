package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.OrderEvaluation;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.service.impl.RunnerEvaluationServiceImpl;
import com.example.backend.vo.RunnerEvaluationSummaryVO;
import com.example.backend.vo.RunnerEvaluationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 跑腿员评价反馈服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RunnerEvaluationService 单元测试")
class RunnerEvaluationServiceTest {

    /**
     * Mock 订单评价 Mapper。
     */
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;

    /**
     * Mock 跑腿员认证 Mapper。
     */
    @Mock
    private RunnerAuthMapper runnerAuthMapper;

    /**
     * 被测跑腿员评价服务。
     */
    @InjectMocks
    private RunnerEvaluationServiceImpl runnerEvaluationService;

    /**
     * 测试用跑腿员用户ID。
     */
    private static final Long RUNNER_ID = 20L;

    /**
     * 构造测试评价记录。
     *
     * @param id        评价ID
     * @param starScore 星级评分
     * @return 评价实体
     */
    private OrderEvaluation buildEvaluation(Long id, Integer starScore) {
        OrderEvaluation evaluation = new OrderEvaluation();
        evaluation.setId(id);
        evaluation.setOrderId(1000L + id);
        evaluation.setPublisherId(10L + id);
        evaluation.setRunnerId(RUNNER_ID);
        evaluation.setStarScore(starScore);
        evaluation.setContent("评价内容" + id);
        evaluation.setIsAnonymous(0);
        evaluation.setCreateTime(LocalDateTime.of(2026, 4, 27, 10, 0).plusMinutes(id));
        evaluation.setIsDeleted(0);
        return evaluation;
    }

    /**
     * 测试分页查询收到评价成功。
     */
    @Test
    @DisplayName("list 成功")
    void shouldListRunnerEvaluationsSuccessfully() {
        mockApprovedRunner();
        OrderEvaluation evaluation = buildEvaluation(1L, 5);
        Page<OrderEvaluation> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(evaluation));
        mapperPage.setTotal(1);
        when(orderEvaluationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mapperPage);

        IPage<RunnerEvaluationVO> result = runnerEvaluationService.listEvaluations(RUNNER_ID, 1, 10, null);

        assertEquals(1, result.getTotal(), "分页总数应来自 Mapper 查询结果");
        assertEquals(1, result.getRecords().size(), "应返回一条评价");
        RunnerEvaluationVO vo = result.getRecords().get(0);
        assertEquals(evaluation.getId(), vo.getId(), "评价ID应正确映射");
        assertEquals(evaluation.getOrderId(), vo.getOrderId(), "订单ID应正确映射");
        assertEquals(evaluation.getPublisherId(), vo.getPublisherId(), "发布人ID应正确映射");
        assertEquals(RUNNER_ID, vo.getRunnerId(), "跑腿员ID应为当前登录用户ID");
        assertEquals(evaluation.getStarScore(), vo.getStarScore(), "评分应正确映射");
        assertEquals(evaluation.getContent(), vo.getContent(), "评价内容应正确映射");
        assertEquals(evaluation.getIsAnonymous(), vo.getIsAnonymous(), "匿名标记应正确映射");
        assertEquals(evaluation.getCreateTime(), vo.getCreateTime(), "创建时间应正确映射");

        verify(orderEvaluationMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    /**
     * 测试分页查询按评分筛选。
     */
    @Test
    @DisplayName("list 按 starScore 筛选")
    void shouldListRunnerEvaluationsWithStarScoreFilter() {
        mockApprovedRunner();
        Page<OrderEvaluation> mapperPage = new Page<>(2, 5);
        mapperPage.setRecords(List.of(buildEvaluation(2L, 4)));
        mapperPage.setTotal(1);
        ArgumentCaptor<Page<OrderEvaluation>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        when(orderEvaluationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mapperPage);

        IPage<RunnerEvaluationVO> result = runnerEvaluationService.listEvaluations(RUNNER_ID, 2, 5, 4);

        assertEquals(2, result.getCurrent(), "当前页应保持请求参数");
        assertEquals(5, result.getSize(), "分页大小应保持请求参数");
        assertEquals(Integer.valueOf(4), result.getRecords().get(0).getStarScore(), "只返回指定评分记录");
        verify(orderEvaluationMapper).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertEquals(2, pageCaptor.getValue().getCurrent(), "传给 Mapper 的页码应正确");
        assertEquals(5, pageCaptor.getValue().getSize(), "传给 Mapper 的页大小应正确");
    }

    /**
     * 测试非法评分筛选失败。
     */
    @Test
    @DisplayName("非法 starScore 失败")
    void shouldThrowWhenStarScoreInvalid() {
        mockApprovedRunner();
        BusinessException exception = assertThrows(BusinessException.class,
                () -> runnerEvaluationService.listEvaluations(RUNNER_ID, 1, 10, 6),
                "评分不在1-5时应抛出业务异常");

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode(), "错误码应为 BAD_REQUEST");
    }

    /**
     * 测试无评价时汇总返回零值。
     */
    @Test
    @DisplayName("summary 空记录")
    void shouldReturnZeroSummaryWhenNoEvaluation() {
        mockApprovedRunner();
        when(orderEvaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        RunnerEvaluationSummaryVO summary = runnerEvaluationService.getSummary(RUNNER_ID);

        assertEquals(BigDecimal.ZERO, summary.getAverageScore(), "无评价时平均分应为0");
        assertEquals(0L, summary.getTotalCount(), "总数应为0");
        assertEquals(0L, summary.getFiveStarCount(), "五星数应为0");
        assertEquals(0L, summary.getFourStarCount(), "四星数应为0");
        assertEquals(0L, summary.getThreeStarCount(), "三星数应为0");
        assertEquals(0L, summary.getTwoStarCount(), "二星数应为0");
        assertEquals(0L, summary.getOneStarCount(), "一星数应为0");
    }

    /**
     * 测试多评分汇总平均分和各星级数量。
     */
    @Test
    @DisplayName("summary 多评分聚合平均分和各星级数量")
    void shouldAggregateSummaryByStarScore() {
        mockApprovedRunner();
        when(orderEvaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                buildEvaluation(1L, 5),
                buildEvaluation(2L, 5),
                buildEvaluation(3L, 4),
                buildEvaluation(4L, 3),
                buildEvaluation(5L, 2),
                buildEvaluation(6L, 1)
        ));

        RunnerEvaluationSummaryVO summary = runnerEvaluationService.getSummary(RUNNER_ID);

        assertEquals(new BigDecimal("3.3"), summary.getAverageScore(), "平均分应保留1位小数");
        assertEquals(6L, summary.getTotalCount(), "总数应正确");
        assertEquals(2L, summary.getFiveStarCount(), "五星数应正确");
        assertEquals(1L, summary.getFourStarCount(), "四星数应正确");
        assertEquals(1L, summary.getThreeStarCount(), "三星数应正确");
        assertEquals(1L, summary.getTwoStarCount(), "二星数应正确");
        assertEquals(1L, summary.getOneStarCount(), "一星数应正确");
    }

    /**
     * 未通过跑腿员认证时拒绝查询评价。
     */
    @Test
    @DisplayName("未认证跑腿员查询失败")
    void shouldThrowWhenRunnerNotApproved() {
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> runnerEvaluationService.listEvaluations(RUNNER_ID, 1, 10, null),
                "未认证跑腿员查询评价时应抛出业务异常");

        assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode(), "错误码应为 FORBIDDEN");
    }

    /**
     * Mock 当前用户为已认证跑腿员。
     */
    private void mockApprovedRunner() {
        RunnerAuth auth = new RunnerAuth();
        auth.setUserId(RUNNER_ID);
        auth.setAuthStatus(AuthStatusEnum.APPROVED.getCode());
        auth.setCurrentFlag(1);
        when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);
    }
}
