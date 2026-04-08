package com.example.backend.controller.runner;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.security.LoginUser;
import com.example.backend.service.RunnerIncomeService;
import com.example.backend.vo.RunnerIncomeOverviewVO;
import com.example.backend.vo.RunnerIncomeRecordVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跑腿员收益查询控制器。
 */
@RestController
@RequestMapping("/api/runner/income")
public class RunnerIncomeController {

    /**
     * 跑腿员收益查询服务。
     */
    private final RunnerIncomeService runnerIncomeService;

    /**
     * 构造函数注入收益服务。
     *
     * @param runnerIncomeService 跑腿员收益查询服务
     */
    public RunnerIncomeController(RunnerIncomeService runnerIncomeService) {
        this.runnerIncomeService = runnerIncomeService;
    }

    /**
     * 查询当前跑腿员收益总览。
     *
     * @param loginUser 当前登录用户
     * @return 收益总览
     */
    @GetMapping("/overview")
    public Result<RunnerIncomeOverviewVO> overview(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(runnerIncomeService.overview(loginUser.getUserId()));
    }

    /**
     * 分页查询当前跑腿员收益明细。
     *
     * @param loginUser        当前登录用户
     * @param page             页码
     * @param size             每页条数
     * @param settlementStatus 结算状态筛选
     * @return 收益明细分页
     */
    @GetMapping("/list")
    public Result<IPage<RunnerIncomeRecordVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) Integer settlementStatus) {
        return Result.success(runnerIncomeService.list(loginUser.getUserId(), page, size, settlementStatus));
    }
}
