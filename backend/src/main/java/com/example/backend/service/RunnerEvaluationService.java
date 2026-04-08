package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.vo.RunnerEvaluationSummaryVO;
import com.example.backend.vo.RunnerEvaluationVO;

/**
 * 跑腿员评价反馈服务接口。
 */
public interface RunnerEvaluationService {

    /**
     * 分页查询跑腿员收到的评价。
     *
     * @param runnerId   当前登录跑腿员用户ID
     * @param page       页码
     * @param size       每页数量
     * @param starScore  可选星级评分筛选
     * @return 跑腿员收到的评价分页结果
     */
    IPage<RunnerEvaluationVO> listEvaluations(Long runnerId, long page, long size, Integer starScore);

    /**
     * 查询跑腿员收到评价的汇总信息。
     *
     * @param runnerId 当前登录跑腿员用户ID
     * @return 评价汇总信息
     */
    RunnerEvaluationSummaryVO getSummary(Long runnerId);
}
