package com.example.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 登录用户信息封装类
 */
public class LoginUser extends User {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录用户信息封装构造函数
     *
     * @param userId      用户ID
     * @param username    用户名
     * @param password    密码
     * @param authorities 权限列表
     */
    public LoginUser(Long userId, String username, String password,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }
}
