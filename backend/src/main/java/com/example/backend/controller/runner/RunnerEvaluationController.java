package com.example.backend.controller.runner;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.security.LoginUser;
import com.example.backend.service.RunnerEvaluationService;
import com.example.backend.vo.RunnerEvaluationSummaryVO;
import com.example.backend.vo.RunnerEvaluationVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跑腿员评价反馈控制器。
 */
@RestController
@RequestMapping("/api/runner/evaluations")
public class RunnerEvaluationController {

    /**
     * 跑腿员评价反馈服务。
     */
    private final RunnerEvaluationService runnerEvaluationService;

    /**
     * 构造函数注入跑腿员评价反馈服务。
     *
     * @param runnerEvaluationService 跑腿员评价反馈服务
     */
    public RunnerEvaluationController(RunnerEvaluationService runnerEvaluationService) {
        this.runnerEvaluationService = runnerEvaluationService;
    }

    /**
     * 分页查询当前跑腿员收到的评价。
     *
     * @param loginUser 当前登录用户
     * @param page      页码，默认1
     * @param size      每页数量，默认10
     * @param starScore 可选星级评分筛选
     * @return 当前跑腿员收到的评价分页结果
     */
    @GetMapping
    public Result<IPage<RunnerEvaluationVO>> listEvaluations(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer starScore) {
        IPage<RunnerEvaluationVO> evaluationPage = runnerEvaluationService.listEvaluations(
                loginUser.getUserId(), page, size, starScore);
        return Result.success(evaluationPage);
    }

    /**
     * 查询当前跑腿员收到评价的汇总信息。
     *
     * @param loginUser 当前登录用户
     * @return 当前跑腿员评价汇总信息
     */
    @GetMapping("/summary")
    public Result<RunnerEvaluationSummaryVO> getSummary(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(runnerEvaluationService.getSummary(loginUser.getUserId()));
    }
}
