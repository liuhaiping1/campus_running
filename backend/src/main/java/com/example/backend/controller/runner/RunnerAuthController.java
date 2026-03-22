package com.example.backend.controller.runner;

import com.example.backend.common.Result;
import com.example.backend.dto.request.RunnerAuthApplyRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.RunnerAuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跑腿员认证控制器（用户端）
 * <p>
 * 处理跑腿员认证申请，需要已登录的STUDENT角色用户访问。
 * </p>
 */
@RestController
@RequestMapping("/api/runner-auth")
public class RunnerAuthController {

    private final RunnerAuthService runnerAuthService;

    /**
     * 构造函数注入跑腿员认证服务
     *
     * @param runnerAuthService 跑腿员认证服务
     */
    public RunnerAuthController(RunnerAuthService runnerAuthService) {
        this.runnerAuthService = runnerAuthService;
    }

    /**
     * 提交跑腿员认证申请
     * <p>
     * 当前登录用户提交跑腿员身份认证申请。
     * 若已存在待审核申请则返回409冲突，
     * 若已通过认证则不允许重复申请。
     * 重新提交时，旧的申请记录会被标记为历史。
     * </p>
     *
     * @param loginUser 当前登录用户信息
     * @param request   认证申请请求（学校信息、证件类型、证件图片等）
     * @return 申请提交结果
     */
    @PostMapping("/apply")
    public Result<String> apply(@AuthenticationPrincipal LoginUser loginUser,
                              @Valid @RequestBody RunnerAuthApplyRequest request) {
        runnerAuthService.apply(loginUser.getUserId(), request);
        return Result.success("认证申请已提交");
    }
}
