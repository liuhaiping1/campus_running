package com.example.backend.service;

import com.example.backend.dto.request.PasswordChangeRequest;
import com.example.backend.dto.request.UserProfileUpdateRequest;
import com.example.backend.vo.RunnerAuthProfileVO;
import com.example.backend.vo.RunnerCenterOverviewVO;
import com.example.backend.vo.UserCenterOverviewVO;
import com.example.backend.vo.UserProfileVO;

/**
 * 用户个人中心服务接口
 * <p>
 * 提供用户资料查询修改、密码修改、学生/跑腿员个人中心概览等能力。
 * </p>
 */
public interface UserProfileService {

    /**
     * 查询当前登录用户资料
     *
     * @param userId 当前用户ID
     * @return 用户资料
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 修改当前登录用户资料
     *
     * @param userId  当前用户ID
     * @param request 资料修改请求
     * @return 修改后的用户资料
     */
    UserProfileVO updateProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 修改当前登录用户密码
     *
     * @param userId  当前用户ID
     * @param request 密码修改请求
     */
    void changePassword(Long userId, PasswordChangeRequest request);

    /**
     * 学生个人中心概览
     *
     * @param userId 当前用户ID
     * @return 概览数据
     */
    UserCenterOverviewVO getStudentOverview(Long userId);

    /**
     * 跑腿员个人中心概览
     *
     * @param userId 当前用户ID
     * @return 概览数据
     */
    RunnerCenterOverviewVO getRunnerOverview(Long userId);

    /**
     * 查询当前用户的跑腿员认证信息（脱敏）
     *
     * @param userId 当前用户ID
     * @return 认证信息，无认证记录时返回null
     */
    RunnerAuthProfileVO getRunnerAuthProfile(Long userId);
}
