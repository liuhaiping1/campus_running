package com.example.backend.controller;

import com.example.backend.common.ErrorCode;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器集成测试
 * <p>
 * 测试用户注册和登录接口，覆盖：
 * <ul>
 *   <li>注册成功</li>
 *   <li>用户名重复</li>
 *   <li>手机号重复</li>
 *   <li>参数校验失败</li>
 *   <li>登录成功</li>
 *   <li>用户名不存在</li>
 *   <li>密码错误</li>
 * </ul>
 */
@DisplayName("认证控制器集成测试")
class AuthControllerTest extends BaseControllerTest {

    /**
     * 构建注册请求
     */
    private RegisterRequest buildRegisterRequest(String username, String password, String realName, String phone) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRealName(realName);
        request.setPhone(phone);
        return request;
    }

    /**
     * 构建登录请求
     */
    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    // =========================================================================
    // 注册测试
    // =========================================================================

    @Nested
    @DisplayName("用户注册")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 - 返回用户ID")
        @Transactional
        void shouldRegisterSuccessfully() throws Exception {
            RegisterRequest request = buildRegisterRequest("testuser01", "password123", "测试用户", "13800138001");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("注册成功"))
                    .andExpect(jsonPath("$.data").isNotEmpty());
        }

        @Test
        @DisplayName("用户名重复 - 返回 USERNAME_EXISTS 错误")
        @Transactional
        void shouldFailWhenUsernameExists() throws Exception {
            // 第一次注册
            RegisterRequest request1 = buildRegisterRequest("duplicate_user", "password123", "用户1", "13800138001");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk());

            // 第二次注册相同用户名
            RegisterRequest request2 = buildRegisterRequest("duplicate_user", "password456", "用户2", "13800138002");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_EXISTS.getCode()));
        }

        @Test
        @DisplayName("手机号重复 - 返回 PHONE_EXISTS 错误")
        @Transactional
        void shouldFailWhenPhoneExists() throws Exception {
            // 第一次注册
            RegisterRequest request1 = buildRegisterRequest("user_a", "password123", "用户A", "13900139001");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk());

            // 第二次注册相同手机号
            RegisterRequest request2 = buildRegisterRequest("user_b", "password456", "用户B", "13900139001");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PHONE_EXISTS.getCode()));
        }

        @Test
        @DisplayName("用户名为空 - 参数校验失败")
        void shouldFailWhenUsernameBlank() throws Exception {
            RegisterRequest request = buildRegisterRequest("", "password123", "测试用户", "13800138001");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1001));
        }

        @Test
        @DisplayName("密码太短 - 参数校验失败")
        void shouldFailWhenPasswordTooShort() throws Exception {
            RegisterRequest request = buildRegisterRequest("testuser02", "12345", "测试用户", "13800138002");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1001));
        }

        @Test
        @DisplayName("手机号格式不正确 - 参数校验失败")
        void shouldFailWhenPhoneInvalid() throws Exception {
            RegisterRequest request = buildRegisterRequest("testuser03", "password123", "测试用户", "12345678901");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1001));
        }
    }

    // =========================================================================
    // 登录测试
    // =========================================================================

    @Nested
    @DisplayName("用户登录")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 返回JWT Token")
        @Transactional
        void shouldLoginSuccessfully() throws Exception {
            // 先注册
            RegisterRequest registerRequest = buildRegisterRequest("loginuser01", "password123", "登录用户", "13700137001");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk());

            // 登录
            LoginRequest loginRequest = buildLoginRequest("loginuser01", "password123");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("登录成功"))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.username").value("loginuser01"));
        }

        @Test
        @DisplayName("用户名不存在 - 返回 USERNAME_OR_PASSWORD_ERROR")
        void shouldFailWhenUserNotFound() throws Exception {
            LoginRequest request = buildLoginRequest("nonexistent_user", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode()));
        }

        @Test
        @DisplayName("密码错误 - 返回 USERNAME_OR_PASSWORD_ERROR")
        @Transactional
        void shouldFailWhenPasswordWrong() throws Exception {
            // 先注册
            RegisterRequest registerRequest = buildRegisterRequest("loginuser02", "password123", "登录用户2", "13700137002");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk());

            // 用错误密码登录
            LoginRequest loginRequest = buildLoginRequest("loginuser02", "wrongpassword");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode()));
        }

        @Test
        @DisplayName("用户名为空 - 参数校验失败")
        void shouldFailWhenUsernameBlank() throws Exception {
            LoginRequest request = buildLoginRequest("", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1001));
        }
    }
}
