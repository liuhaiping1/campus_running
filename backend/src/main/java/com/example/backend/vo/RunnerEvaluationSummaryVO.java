package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 跑腿员收到评价的汇总视图对象。
 */
@Data
@Builder
public class RunnerEvaluationSummaryVO {

    /**
     * 平均评分，保留1位小数。
     */
    private BigDecimal averageScore;

    /**
     * 评价总数。
     */
    private Long totalCount;

    /**
     * 五星评价数量。
     */
    private Long fiveStarCount;

    /**
     * 四星评价数量。
     */
    private Long fourStarCount;

    /**
     * 三星评价数量。
     */
    private Long threeStarCount;

    /**
     * 二星评价数量。
     */
    private Long twoStarCount;

    /**
     * 一星评价数量。
     */
    private Long oneStarCount;
}
