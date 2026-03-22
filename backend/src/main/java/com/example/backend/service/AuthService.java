package com.example.backend.service;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.vo.LoginResponse;

/**
 * 认证服务接口
 *
 * @author campus_running
 */
public interface AuthService {

    /**
     * 用户注册
     * <p>
     * 校验用户名和手机号唯一性后创建用户，
     * 同时默认授予STUDENT角色。
     *
     * @param request 注册请求（包含用户名、密码、真实姓名、手机号）
     * @return 注册成功的用户ID
     * @throws com.example.backend.common.exception.BusinessException 用户名已存在或手机号已存在时抛出
     */
    Long register(RegisterRequest request);

    /**
     * 用户登录
     * <p>
     * 校验账号密码，更新最后登录时间，
     * 生成JWT令牌并返回用户信息。
     *
     * @param request 登录请求（包含用户名和密码）
     * @return 登录响应（含JWT令牌和用户基本信息）
     * @throws com.example.backend.common.exception.BusinessException 用户名或密码错误、账号被禁用时抛出
     */
    LoginResponse login(LoginRequest request);
}
