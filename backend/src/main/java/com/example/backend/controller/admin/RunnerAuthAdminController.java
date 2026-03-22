package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.dto.request.RunnerAuthReviewRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.RunnerAuthService;
import com.example.backend.vo.RunnerAuthVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跑腿员认证管理控制器（管理员端）
 * <p>
 * 处理管理员对跑腿员认证申请的审核和查询，需要ADMIN角色。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/runner-auth")
public class RunnerAuthAdminController {

    private final RunnerAuthService runnerAuthService;

    /**
     * 构造函数注入跑腿员认证服务
     *
     * @param runnerAuthService 跑腿员认证服务
     */
    public RunnerAuthAdminController(RunnerAuthService runnerAuthService) {
        this.runnerAuthService = runnerAuthService;
    }

    /**
     * 审核跑腿员认证申请
     * <p>
     * 管理员对认证申请进行审核，审核通过后自动授予RUNNER角色。
     * 驳回时需填写驳回原因。
     * </p>
     *
     * @param id        认证记录ID
     * @param loginUser 当前登录的管理员信息
     * @param request   审核请求（审核结果、驳回原因）
     * @return 审核结果
     */
    @PostMapping("/{id}/review")
    public Result<String> review(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser,
                               @Valid @RequestBody RunnerAuthReviewRequest request) {
        runnerAuthService.review(id, loginUser.getUserId(), request);
        return Result.success("审核完成");
    }

    /**
     * 分页查询跑腿员认证申请列表
     * <p>
     * 支持按认证状态筛选，返回申请人基本信息与认证详情。
     * </p>
     *
     * @param page       页码，默认1
     * @param size       每页大小，默认10
     * @param authStatus 认证状态筛选（0待审 1通过 2驳回 3失效），可选
     * @return 分页认证申请列表
     */
    @GetMapping("/list")
    public Result<IPage<RunnerAuthVO>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) Integer authStatus) {
        IPage<RunnerAuthVO> result = runnerAuthService.list(page, size, authStatus);
        return Result.success(result);
    }
}
