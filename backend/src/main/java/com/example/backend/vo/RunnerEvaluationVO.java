package com.example.backend.vo;

import com.example.backend.entity.OrderEvaluation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跑腿员收到的评价视图对象。
 */
@Data
@Builder
public class RunnerEvaluationVO {

    /**
     * 评价ID。
     */
    private Long id;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 发布人ID。
     */
    private Long publisherId;

    /**
     * 跑腿员ID。
     */
    private Long runnerId;

    /**
     * 星级评分，范围1-5。
     */
    private Integer starScore;

    /**
     * 评价内容。
     */
    private String content;

    /**
     * 是否匿名。
     */
    private Integer isAnonymous;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 根据评价实体构建跑腿员评价视图。
     *
     * @param evaluation 评价实体
     * @return 跑腿员评价视图对象
     */
    public static RunnerEvaluationVO from(OrderEvaluation evaluation) {
        return RunnerEvaluationVO.builder()
                .id(evaluation.getId())
                .orderId(evaluation.getOrderId())
                .publisherId(evaluation.getPublisherId())
                .runnerId(evaluation.getRunnerId())
                .starScore(evaluation.getStarScore())
                .content(evaluation.getContent())
                .isAnonymous(evaluation.getIsAnonymous())
                .createTime(evaluation.getCreateTime())
                .build();
    }
}
