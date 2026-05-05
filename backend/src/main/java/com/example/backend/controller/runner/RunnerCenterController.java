package com.example.backend.controller.runner;

import com.example.backend.common.Result;
import com.example.backend.security.LoginUser;
import com.example.backend.service.UserProfileService;
import com.example.backend.vo.RunnerAuthProfileVO;
import com.example.backend.vo.RunnerCenterOverviewVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跑腿员个人中心控制器
 * <p>
 * 处理跑腿员个人中心概览和认证信息查询等请求。
 * </p>
 */
@RestController
@RequestMapping("/api/runner")
public class RunnerCenterController {

    private final UserProfileService userProfileService;

    /**
     * 构造函数注入用户个人中心服务
     *
     * @param userProfileService 用户个人中心服务
     */
    public RunnerCenterController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 跑腿员个人中心概览
     *
     * @param loginUser 当前登录用户信息
     * @return 概览数据
     */
    @GetMapping("/center/overview")
    public Result<RunnerCenterOverviewVO> getRunnerOverview(@AuthenticationPrincipal LoginUser loginUser) {
        RunnerCenterOverviewVO overview = userProfileService.getRunnerOverview(loginUser.getUserId());
        return Result.success(overview);
    }

    /**
     * 查询当前用户的跑腿员认证信息
     *
     * @param loginUser 当前登录用户信息
     * @return 认证信息，无认证记录时返回null
     */
    @GetMapping("/profile/auth")
    public Result<RunnerAuthProfileVO> getRunnerAuthProfile(@AuthenticationPrincipal LoginUser loginUser) {
        RunnerAuthProfileVO authProfile = userProfileService.getRunnerAuthProfile(loginUser.getUserId());
        return Result.success(authProfile);
    }
}
