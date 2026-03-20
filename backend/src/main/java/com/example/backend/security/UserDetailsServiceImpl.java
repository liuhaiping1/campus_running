package com.example.backend.security;

import com.example.backend.common.ErrorCode;
import com.example.backend.entity.SysUser;
import com.example.backend.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户认证服务实现类
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getIsDeleted, 0)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查账号是否被禁用
        if (user.getUserStatus() != 1) {
            throw new UsernameNotFoundException("账号已被禁用");
        }

        // 查询用户角色
        List<String> roles = sysUserMapper.selectUserRoles(user.getId())
                .stream()
                .map(SysUserMapper.UserRole::getRoleCode)
                .collect(Collectors.toList());

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), authorities);
    }
}
