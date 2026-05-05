package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.entity.SysRole;
import com.example.backend.entity.SysUser;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.SysRoleMapper;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.security.JwtTokenUtil;
import com.example.backend.service.AuthService;
import com.example.backend.vo.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 * <p>
 * 实现用户注册与登录的核心业务逻辑。
 * 使用MyBatis-Plus的LambdaQueryWrapper进行数据库查询，
 * 通过BCryptPasswordEncoder加密密码，
 * 通过JwtTokenUtil生成JWT令牌。
 *
 * @author campus_running
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 构造函数注入依赖
     *
     * @param sysUserMapper     系统用户Mapper
     * @param sysRoleMapper     系统角色Mapper
     * @param sysUserRoleMapper 用户角色关联Mapper
     * @param passwordEncoder   密码编码器（BCrypt）
     * @param jwtTokenUtil      JWT令牌工具类
     */
    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           SysRoleMapper sysRoleMapper,
                           SysUserRoleMapper sysUserRoleMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenUtil jwtTokenUtil) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 用户注册
     * <p>
     * 执行步骤：
     * 1. 校验用户名是否已存在
     * 2. 校验手机号是否已存在
     * 3. 创建并保存用户记录
     * 4. 查询STUDENT角色
     * 5. 为用户授予STUDENT角色
     *
     * @param request 注册请求（包含用户名、密码、真实姓名、手机号）
     * @return 注册成功的用户ID
     * @throws BusinessException 用户名已存在（USERNAME_EXISTS）或手机号已存在（PHONE_EXISTS）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        // 1. 校验用户名是否已存在（逻辑删除由MyBatis-Plus @TableLogic自动处理）
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, request.getUsername());
        if (sysUserMapper.selectCount(usernameWrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 2. 校验手机号是否已存在
        LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(SysUser::getPhone, request.getPhone());
        if (sysUserMapper.selectCount(phoneWrapper) > 0) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        // 3. 创建新用户
        SysUser sysUser = new SysUser();
        LocalDateTime now = LocalDateTime.now();
        sysUser.setUsername(request.getUsername());
        sysUser.setPassword(passwordEncoder.encode(request.getPassword()));
        sysUser.setRealName(request.getRealName());
        sysUser.setPhone(request.getPhone());
        sysUser.setUserStatus(1);
        sysUser.setCreateTime(now);
        sysUser.setUpdateTime(now);
        sysUserMapper.insert(sysUser);

        // 4. 查询STUDENT角色（仅启用的角色，逻辑删除由@TableLogic自动过滤）
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getRoleCode, "STUDENT")
                    .eq(SysRole::getRoleStatus, 1);
        SysRole studentRole = sysRoleMapper.selectOne(roleWrapper);
        if (studentRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "STUDENT角色不存在或已停用，请联系管理员");
        }

        // 5. 为用户授予STUDENT角色
        SysUserRole sysUserRole = new SysUserRole();
        sysUserRole.setUserId(sysUser.getId());
        sysUserRole.setRoleId(studentRole.getId());
        sysUserRole.setRoleCode("STUDENT");
        sysUserRole.setGrantSource(1);
        sysUserRole.setGrantTime(now);
        sysUserRole.setRoleStatus(1);
        sysUserRole.setCreateTime(now);
        sysUserRole.setUpdateTime(now);
        sysUserRoleMapper.insert(sysUserRole);

        return sysUser.getId();
    }

    /**
     * 用户登录
     * <p>
     * 执行步骤：
     * 1. 根据用户名查询用户
     * 2. 校验账号状态是否正常
     * 3. 校验密码是否正确
     * 4. 更新最后登录时间
     * 5. 查询用户角色列表
     * 6. 生成JWT令牌
     * 7. 组装并返回登录响应
     *
     * @param request 登录请求（包含用户名和密码）
     * @return 登录响应（含JWT令牌、用户ID、用户名、真实姓名、角色列表）
     * @throws BusinessException 用户名或密码错误（USERNAME_OR_PASSWORD_ERROR）或账号已禁用（ACCOUNT_DISABLED）
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 根据用户名查询用户（逻辑删除由@TableLogic自动过滤）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser sysUser = sysUserMapper.selectOne(wrapper);
        if (sysUser == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 2. 校验账号是否被禁用
        if (sysUser.getUserStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), sysUser.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 4. 更新最后登录时间
        LocalDateTime now = LocalDateTime.now();
        sysUser.setLastLoginTime(now);
        sysUser.setUpdateTime(now);
        sysUserMapper.updateById(sysUser);

        // 5. 查询用户角色列表
        List<SysUserMapper.UserRole> userRoles = sysUserMapper.selectUserRoles(sysUser.getId());
        List<String> roles = userRoles.stream()
                .map(SysUserMapper.UserRole::getRoleCode)
                .collect(Collectors.toList());

        // 6. 生成JWT令牌
        String token = jwtTokenUtil.generateToken(sysUser.getId(), sysUser.getUsername(), roles);

        // 7. 组装并返回登录响应
        return LoginResponse.builder()
                .token(token)
                .userId(sysUser.getId())
                .username(sysUser.getUsername())
                .realName(sysUser.getRealName())
                .roles(roles)
                .build();
    }
}
