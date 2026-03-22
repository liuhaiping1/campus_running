package com.example.backend.controller.auth;

import com.example.backend.common.Result;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.service.AuthService;
import com.example.backend.vo.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * <p>
 * 处理用户注册和登录请求，无需认证即可访问。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 构造函数注入认证服务
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册
     * <p>
     * 创建新用户账号并默认授予STUDENT角色。
     * 用户名和手机号必须唯一，密码经BCrypt加密存储。
     * </p>
     *
     * @param request 注册请求（用户名、密码、真实姓名、手机号）
     * @return 注册成功的用户ID
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.success("注册成功", userId);
    }

    /**
     * 用户登录
     * <p>
     * 验证用户名和密码，成功后返回JWT令牌和用户基本信息。
     * 令牌有效期24小时，后续请求需在Authorization头中携带。
     * </p>
     *
     * @param request 登录请求（用户名、密码）
     * @return 登录响应（含JWT令牌、用户ID、用户名、真实姓名、角色列表）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }
}
