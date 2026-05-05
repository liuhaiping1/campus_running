package com.example.backend.controller;

import com.example.backend.security.JwtTokenUtil;
import com.example.backend.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

/**
 * Controller层集成测试基类
 * <p>
 * 提供通用的测试工具方法，包括：
 * <ul>
 *   <li>MockMvc 自动注入</li>
 *   <li>ObjectMapper 自动注入</li>
 *   <li>JWT Token 生成工具方法</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenUtil jwtTokenUtil;

    @Autowired
    protected UserDetailsService userDetailsService;

    /**
     * 为指定用户生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色列表
     * @return JWT Token字符串
     */
    protected String generateToken(Long userId, String username, List<String> roles) {
        return jwtTokenUtil.generateToken(userId, username, roles);
    }

    /**
     * 为指定用户名生成JWT Token（从数据库加载用户信息）
     *
     * @param username 用户名
     * @return JWT Token字符串
     */
    protected String generateToken(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails instanceof LoginUser loginUser) {
            return jwtTokenUtil.generateToken(loginUser.getUserId(), username,
                    userDetails.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList());
        }
        return jwtTokenUtil.generateToken(0L, username, List.of());
    }

    /**
     * 构建Authorization头
     *
     * @param username 用户名
     * @return Bearer Token字符串
     */
    protected String bearerToken(String username) {
        return "Bearer " + generateToken(username);
    }

    /**
     * 构建Authorization头
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色列表
     * @return Bearer Token字符串
     */
    protected String bearerToken(Long userId, String username, List<String> roles) {
        return "Bearer " + generateToken(userId, username, roles);
    }
}
