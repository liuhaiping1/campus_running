package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.RunnerAuthApplyRequest;
import com.example.backend.dto.request.RunnerAuthReviewRequest;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.SysRole;
import com.example.backend.entity.SysUser;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.SysRoleMapper;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.service.RunnerAuthService;
import com.example.backend.vo.RunnerAuthVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 跑腿员认证服务实现类
 * <p>
 * 负责处理跑腿员认证申请、审核和查询业务逻辑。
 * 所有写操作均在事务中执行，保障数据一致性。
 * </p>
 */
@Service
public class RunnerAuthServiceImpl implements RunnerAuthService {

    private final RunnerAuthMapper runnerAuthMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 构造函数注入依赖Mapper
     *
     * @param runnerAuthMapper    跑腿员认证Mapper
     * @param sysUserMapper       系统用户Mapper
     * @param sysRoleMapper       系统角色Mapper
     * @param sysUserRoleMapper   用户角色关联Mapper
     */
    public RunnerAuthServiceImpl(RunnerAuthMapper runnerAuthMapper,
                                  SysUserMapper sysUserMapper,
                                  SysRoleMapper sysRoleMapper,
                                  SysUserRoleMapper sysUserRoleMapper) {
        this.runnerAuthMapper = runnerAuthMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    /**
     * 提交跑腿员认证申请
     * <p>
     * 执行流程：
     * <ol>
     *   <li>校验是否存在待审核的认证申请，存在则抛出业务异常</li>
     *   <li>校验是否已通过认证，已通过则抛出业务异常</li>
     *   <li>将用户所有当前生效的认证记录标记为历史（currentFlag=0）</li>
     *   <li>生成认证批次号（UUID去除横线）</li>
     *   <li>创建新的待审核认证记录并保存</li>
     * </ol>
     * </p>
     *
     * @param userId  申请人用户ID
     * @param request 认证申请请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, RunnerAuthApplyRequest request) {
        // 1. 检查是否存在待审核的认证申请
        LambdaQueryWrapper<RunnerAuth> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getAuthStatus, AuthStatusEnum.PENDING.getCode())
                .eq(RunnerAuth::getCurrentFlag, 1);
        if (runnerAuthMapper.selectCount(pendingWrapper) > 0) {
            throw new BusinessException(ErrorCode.AUTH_PENDING_EXISTS);
        }

        // 2. 检查是否已通过认证
        LambdaQueryWrapper<RunnerAuth> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getAuthStatus, AuthStatusEnum.APPROVED.getCode())
                .eq(RunnerAuth::getCurrentFlag, 1);
        if (runnerAuthMapper.selectCount(approvedWrapper) > 0) {
            throw new BusinessException(ErrorCode.AUTH_ALREADY_APPROVED);
        }

        // 3a. 将用户所有当前生效的认证记录标记为历史
        LambdaUpdateWrapper<RunnerAuth> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();
        updateWrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getCurrentFlag, 1)
                .set(RunnerAuth::getCurrentFlag, 0)
                .set(RunnerAuth::getUpdateTime, now);
        runnerAuthMapper.update(null, updateWrapper);

        // 3b. 生成认证批次号（UUID去除横线）
        String authBatchNo = UUID.randomUUID().toString().replace("-", "");

        // 3c. 创建新的认证申请记录
        RunnerAuth auth = new RunnerAuth();
        auth.setUserId(userId);
        auth.setAuthBatchNo(authBatchNo);
        auth.setSchoolName(request.getSchoolName());
        auth.setCampusName(request.getCampusName());
        auth.setCertType(request.getCertType());
        auth.setCertNo(request.getCertNo());
        auth.setCertFrontUrl(request.getCertFrontUrl());
        auth.setCertBackUrl(request.getCertBackUrl());
        auth.setAuthStatus(AuthStatusEnum.PENDING.getCode());
        auth.setCurrentFlag(1);
        auth.setCreateTime(now);
        auth.setUpdateTime(now);

        // 3d. 保存认证记录
        runnerAuthMapper.insert(auth);
    }

    /**
     * 审核跑腿员认证申请
     * <p>
     * 执行流程：
     * <ol>
     *   <li>根据ID查询认证记录，不存在则抛出业务异常</li>
     *   <li>校验申请状态必须为待审核，否则抛出状态冲突异常</li>
     *   <li>设置审核管理员、审核时间和审核结果</li>
     *   <li>驳回时设置驳回原因</li>
     *   <li>审核通过时，检查并授予RUNNER角色（认证来源）</li>
     *   <li>更新认证记录</li>
     * </ol>
     * </p>
     *
     * @param id       认证记录ID
     * @param adminId  审核管理员ID
     * @param request  审核请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, Long adminId, RunnerAuthReviewRequest request) {
        // 1. 根据ID查询认证记录
        RunnerAuth auth = runnerAuthMapper.selectById(id);
        if (auth == null) {
            throw new BusinessException(ErrorCode.AUTH_NOT_FOUND);
        }

        // 2. 校验认证状态必须为待审核
        if (!AuthStatusEnum.PENDING.getCode().equals(auth.getAuthStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该认证申请已审核，无法重复操作");
        }

        // 3. 设置审核信息
        auth.setReviewAdminId(adminId);
        LocalDateTime now = LocalDateTime.now();
        auth.setReviewTime(now);
        auth.setAuthStatus(request.getAuthStatus());
        auth.setUpdateTime(now);

        // 4. 驳回时设置驳回原因
        if (AuthStatusEnum.REJECTED.getCode().equals(request.getAuthStatus())) {
            auth.setRejectReason(request.getRejectReason());
        }

        // 5. 审核通过时，检查并授予RUNNER角色
        if (AuthStatusEnum.APPROVED.getCode().equals(request.getAuthStatus())) {
            grantRunnerRoleIfAbsent(auth.getUserId());
        }

        // 6. 更新认证记录
        runnerAuthMapper.updateById(auth);
    }

    /**
     * 为用户授予RUNNER角色（如果尚未拥有）
     * <p>
     * 先检查用户是否已拥有有效的RUNNER角色，
     * 若没有则查询sys_role获取RUNNER角色信息并创建新的用户角色关联。
     * 授权来源标记为"认证通过"（grantSource=2）。
     * </p>
     *
     * @param userId 用户ID
     */
    private void grantRunnerRoleIfAbsent(Long userId) {
        // 检查用户是否已拥有有效的RUNNER角色
        LambdaQueryWrapper<SysUserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleCode, "RUNNER")
                .eq(SysUserRole::getRoleStatus, 1);
        if (sysUserRoleMapper.selectCount(roleWrapper) > 0) {
            return;
        }

        // 查询RUNNER角色信息
        LambdaQueryWrapper<SysRole> sysRoleWrapper = new LambdaQueryWrapper<>();
        sysRoleWrapper.eq(SysRole::getRoleCode, "RUNNER")
                .eq(SysRole::getRoleStatus, 1);
        SysRole runnerRole = sysRoleMapper.selectOne(sysRoleWrapper);
        if (runnerRole == null) {
            return;
        }

        // 创建用户角色关联（授权来源：2-认证通过）
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(runnerRole.getId());
        userRole.setRoleCode("RUNNER");
        userRole.setGrantSource(2);
        LocalDateTime now = LocalDateTime.now();
        userRole.setGrantTime(now);
        userRole.setRoleStatus(1);
        userRole.setCreateTime(now);
        userRole.setUpdateTime(now);
        sysUserRoleMapper.insert(userRole);
    }

    /**
     * 分页查询跑腿员认证申请列表
     * <p>
     * 执行流程：
     * <ol>
     *   <li>构建查询条件，支持按认证状态筛选</li>
     *   <li>按创建时间倒序分页查询认证记录</li>
     *   <li>批量查询关联的用户信息（姓名、手机号）</li>
     *   <li>组装RunnerAuthVO视图对象列表</li>
     *   <li>返回包含分页信息和VO列表的分页结果</li>
     * </ol>
     * </p>
     *
     * @param page       页码，从1开始
     * @param size       每页大小
     * @param authStatus 认证状态筛选，null表示查询全部
     * @return 分页认证视图对象
     */
    @Override
    public IPage<RunnerAuthVO> list(int page, int size, Integer authStatus) {
        // 构建分页查询条件
        Page<RunnerAuth> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RunnerAuth> wrapper = new LambdaQueryWrapper<>();
        if (authStatus != null) {
            wrapper.eq(RunnerAuth::getAuthStatus, authStatus);
        }
        wrapper.orderByDesc(RunnerAuth::getCreateTime);

        // 分页查询认证记录
        Page<RunnerAuth> authPage = runnerAuthMapper.selectPage(pageParam, wrapper);

        // 提取所有不重复的用户ID
        List<Long> userIds = authPage.getRecords().stream()
                .map(RunnerAuth::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息，构建用户ID到用户的映射
        Map<Long, SysUser> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u, (u1, u2) -> u1));
        }

        // 组装VO列表
        final Map<Long, SysUser> finalUserMap = userMap;
        List<RunnerAuthVO> voList = authPage.getRecords().stream()
                .map(auth -> buildRunnerAuthVO(auth, finalUserMap))
                .collect(Collectors.toList());

        // 构建分页结果
        Page<RunnerAuthVO> resultPage = new Page<>(authPage.getCurrent(), authPage.getSize(), authPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据认证实体和用户映射构建视图对象
     *
     * @param auth    认证实体
     * @param userMap 用户ID到用户实体的映射
     * @return 认证视图对象
     */
    private RunnerAuthVO buildRunnerAuthVO(RunnerAuth auth, Map<Long, SysUser> userMap) {
        RunnerAuthVO.RunnerAuthVOBuilder builder = RunnerAuthVO.builder()
                .id(auth.getId())
                .userId(auth.getUserId())
                .authBatchNo(auth.getAuthBatchNo())
                .schoolName(auth.getSchoolName())
                .campusName(auth.getCampusName())
                .certType(auth.getCertType())
                .certNo(auth.getCertNo())
                .certFrontUrl(auth.getCertFrontUrl())
                .certBackUrl(auth.getCertBackUrl())
                .authStatus(auth.getAuthStatus())
                .rejectReason(auth.getRejectReason())
                .reviewAdminId(auth.getReviewAdminId())
                .reviewTime(auth.getReviewTime())
                .createTime(auth.getCreateTime());

        // 填充关联的用户信息
        SysUser user = userMap.get(auth.getUserId());
        if (user != null) {
            builder.realName(user.getRealName())
                    .phone(user.getPhone());
        }

        return builder.build();
    }
}
