package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.vo.RunnerIncomeOverviewVO;
import com.example.backend.vo.RunnerIncomeRecordVO;

/**
 * 跑腿员收益查询服务。
 */
public interface RunnerIncomeService {

    /**
     * 查询当前跑腿员收益总览。
     *
     * @param userId 当前登录用户ID
     * @return 收益总览
     */
    RunnerIncomeOverviewVO overview(Long userId);

    /**
     * 分页查询当前跑腿员收益明细。
     *
     * @param userId           当前登录用户ID
     * @param page             页码
     * @param size             每页条数
     * @param settlementStatus 结算状态筛选，null 表示全部
     * @return 收益明细分页
     */
    IPage<RunnerIncomeRecordVO> list(Long userId, int page, int size, Integer settlementStatus);
}
