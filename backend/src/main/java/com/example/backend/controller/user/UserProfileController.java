package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.dto.request.PasswordChangeRequest;
import com.example.backend.dto.request.UserProfileUpdateRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.UserProfileService;
import com.example.backend.vo.RunnerAuthProfileVO;
import com.example.backend.vo.RunnerCenterOverviewVO;
import com.example.backend.vo.UserCenterOverviewVO;
import com.example.backend.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人中心控制器
 * <p>
 * 处理用户资料查询修改、密码修改、个人中心概览等请求。
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 构造函数注入用户个人中心服务
     *
     * @param userProfileService 用户个人中心服务
     */
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 查询当前登录用户资料
     *
     * @param loginUser 当前登录用户信息
     * @return 用户资料
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        UserProfileVO profile = userProfileService.getProfile(loginUser.getUserId());
        return Result.success(profile);
    }

    /**
     * 修改当前登录用户资料
     *
     * @param loginUser 当前登录用户信息
     * @param request   资料修改请求
     * @return 修改后的用户资料
     */
    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                               @Valid @RequestBody UserProfileUpdateRequest request) {
        UserProfileVO profile = userProfileService.updateProfile(loginUser.getUserId(), request);
        return Result.success("资料修改成功", profile);
    }

    /**
     * 修改当前登录用户密码
     *
     * @param loginUser 当前登录用户信息
     * @param request   密码修改请求
     * @return 操作结果
     */
    @PostMapping("/password/change")
    public Result<String> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody PasswordChangeRequest request) {
        userProfileService.changePassword(loginUser.getUserId(), request);
        return Result.success("密码修改成功");
    }

    /**
     * 学生个人中心概览
     *
     * @param loginUser 当前登录用户信息
     * @return 概览数据
     */
    @GetMapping("/center/overview")
    public Result<UserCenterOverviewVO> getStudentOverview(@AuthenticationPrincipal LoginUser loginUser) {
        UserCenterOverviewVO overview = userProfileService.getStudentOverview(loginUser.getUserId());
        return Result.success(overview);
    }
}
