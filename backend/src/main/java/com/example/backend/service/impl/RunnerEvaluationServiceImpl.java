package com.example.backend.service.impl;

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
import com.example.backend.service.RunnerEvaluationService;
import com.example.backend.vo.RunnerEvaluationSummaryVO;
import com.example.backend.vo.RunnerEvaluationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 跑腿员评价反馈服务实现类。
 */
@Service
@RequiredArgsConstructor
public class RunnerEvaluationServiceImpl implements RunnerEvaluationService {

    /**
     * 订单评价 Mapper。
     */
    private final OrderEvaluationMapper orderEvaluationMapper;

    /**
     * 跑腿员认证 Mapper。
     */
    private final RunnerAuthMapper runnerAuthMapper;

    /**
     * 分页查询跑腿员收到的评价。
     *
     * @param runnerId  当前登录跑腿员用户ID
     * @param page      页码
     * @param size      每页数量
     * @param starScore 可选星级评分筛选
     * @return 跑腿员收到的评价分页结果
     */
    @Override
    public IPage<RunnerEvaluationVO> listEvaluations(Long runnerId, long page, long size, Integer starScore) {
        resolveRunnerId(runnerId);
        validatePage(page, size);
        validateStarScore(starScore);

        LambdaQueryWrapper<OrderEvaluation> queryWrapper = buildBaseWrapper(runnerId);
        if (starScore != null) {
            queryWrapper.eq(OrderEvaluation::getStarScore, starScore);
        }
        queryWrapper.orderByDesc(OrderEvaluation::getCreateTime);

        IPage<OrderEvaluation> evaluationPage = orderEvaluationMapper.selectPage(new Page<>(page, size), queryWrapper);
        return evaluationPage.convert(RunnerEvaluationVO::from);
    }

    /**
     * 查询跑腿员收到评价的汇总信息。
     *
     * @param runnerId 当前登录跑腿员用户ID
     * @return 评价汇总信息
     */
    @Override
    public RunnerEvaluationSummaryVO getSummary(Long runnerId) {
        resolveRunnerId(runnerId);

        List<OrderEvaluation> evaluations = orderEvaluationMapper.selectList(buildBaseWrapper(runnerId));
        long totalCount = evaluations.size();
        if (totalCount == 0) {
            return buildSummary(BigDecimal.ZERO, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        long fiveStarCount = countByStar(evaluations, 5);
        long fourStarCount = countByStar(evaluations, 4);
        long threeStarCount = countByStar(evaluations, 3);
        long twoStarCount = countByStar(evaluations, 2);
        long oneStarCount = countByStar(evaluations, 1);
        long validScoreCount = evaluations.stream()
                .map(OrderEvaluation::getStarScore)
                .filter(score -> score != null && score >= 1 && score <= 5)
                .count();
        int totalScore = evaluations.stream()
                .map(OrderEvaluation::getStarScore)
                .filter(score -> score != null && score >= 1 && score <= 5)
                .mapToInt(Integer::intValue)
                .sum();
        BigDecimal averageScore = validScoreCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalScore).divide(BigDecimal.valueOf(validScoreCount), 1, RoundingMode.HALF_UP);

        return buildSummary(averageScore, totalCount, fiveStarCount, fourStarCount,
                threeStarCount, twoStarCount, oneStarCount);
    }

    /**
     * 构建跑腿员评价基础查询条件。
     *
     * @param runnerId 当前登录跑腿员用户ID
     * @return 基础查询条件
     */
    private LambdaQueryWrapper<OrderEvaluation> buildBaseWrapper(Long runnerId) {
        return new LambdaQueryWrapper<OrderEvaluation>()
                .eq(OrderEvaluation::getRunnerId, runnerId)
                .eq(OrderEvaluation::getIsDeleted, 0);
    }

    /**
     * 解析并校验当前跑腿员用户ID。
     *
     * @param runnerId 当前登录用户ID
     * @return 跑腿员用户ID
     */
    private Long resolveRunnerId(Long runnerId) {
        if (runnerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<RunnerAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunnerAuth::getUserId, runnerId)
                .eq(RunnerAuth::getAuthStatus, AuthStatusEnum.APPROVED.getCode())
                .eq(RunnerAuth::getCurrentFlag, 1);
        RunnerAuth runnerAuth = runnerAuthMapper.selectOne(wrapper);
        if (runnerAuth == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return runnerAuth.getUserId();
    }

    /**
     * 校验分页参数。
     *
     * @param page 页码
     * @param size 每页数量
     */
    private void validatePage(long page, long size) {
        if (page < 1 || size < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 校验评分筛选参数。
     *
     * @param starScore 评分筛选
     */
    private void validateStarScore(Integer starScore) {
        if (starScore != null && (starScore < 1 || starScore > 5)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 统计指定星级数量。
     *
     * @param evaluations 评价列表
     * @param starScore   星级评分
     * @return 指定星级评价数量
     */
    private long countByStar(List<OrderEvaluation> evaluations, int starScore) {
        return evaluations.stream()
                .filter(evaluation -> Integer.valueOf(starScore).equals(evaluation.getStarScore()))
                .count();
    }

    /**
     * 构建评价汇总视图。
     *
     * @param averageScore   平均评分
     * @param totalCount     总评价数量
     * @param fiveStarCount  五星数量
     * @param fourStarCount  四星数量
     * @param threeStarCount 三星数量
     * @param twoStarCount   二星数量
     * @param oneStarCount   一星数量
     * @return 评价汇总视图对象
     */
    private RunnerEvaluationSummaryVO buildSummary(BigDecimal averageScore,
                                                   long totalCount,
                                                   long fiveStarCount,
                                                   long fourStarCount,
                                                   long threeStarCount,
                                                   long twoStarCount,
                                                   long oneStarCount) {
        return RunnerEvaluationSummaryVO.builder()
                .averageScore(averageScore)
                .totalCount(totalCount)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .build();
    }
}
