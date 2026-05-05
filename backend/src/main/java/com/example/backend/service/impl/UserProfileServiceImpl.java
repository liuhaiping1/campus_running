package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.SettlementStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.PasswordChangeRequest;
import com.example.backend.dto.request.UserProfileUpdateRequest;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.service.UserProfileService;
import com.example.backend.vo.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户个人中心服务实现类
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final SysUserMapper sysUserMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final StationMessageMapper stationMessageMapper;
    private final RunnerAuthMapper runnerAuthMapper;
    private final RunnerIncomeRecordMapper runnerIncomeRecordMapper;
    private final OrderEvaluationMapper orderEvaluationMapper;
    private final PasswordEncoder passwordEncoder;

    public UserProfileServiceImpl(SysUserMapper sysUserMapper,
                                  ErrandOrderMapper errandOrderMapper,
                                  StationMessageMapper stationMessageMapper,
                                  RunnerAuthMapper runnerAuthMapper,
                                  RunnerIncomeRecordMapper runnerIncomeRecordMapper,
                                  OrderEvaluationMapper orderEvaluationMapper,
                                  PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.errandOrderMapper = errandOrderMapper;
        this.stationMessageMapper = stationMessageMapper;
        this.runnerAuthMapper = runnerAuthMapper;
        this.runnerIncomeRecordMapper = runnerIncomeRecordMapper;
        this.orderEvaluationMapper = orderEvaluationMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询当前登录用户资料
     *
     * @param userId 当前用户ID
     * @return 用户资料
     */
    @Override
    public UserProfileVO getProfile(Long userId) {
        SysUser user = requireUser(userId);
        List<String> roles = getUserRoles(userId);
        return UserProfileVO.from(user, roles);
    }

    /**
     * 修改当前登录用户资料
     *
     * @param userId  当前用户ID
     * @param request 资料修改请求
     * @return 修改后的用户资料
     */
    @Override
    public UserProfileVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        SysUser user = requireUser(userId);

        // 手机号唯一性校验：如果修改了手机号，需检查是否已被其他人使用
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, request.getPhone())
                    .ne(SysUser::getId, userId);
            if (sysUserMapper.selectCount(phoneWrapper) > 0) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
            user.setPhone(request.getPhone());
        }

        // 更新允许修改的字段
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getNickName() != null) {
            user.setNickName(request.getNickName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        sysUserMapper.updateById(user);

        List<String> roles = getUserRoles(userId);
        return UserProfileVO.from(user, roles);
    }

    /**
     * 修改当前登录用户密码
     *
     * @param userId  当前用户ID
     * @param request 密码修改请求
     */
    @Override
    public void changePassword(Long userId, PasswordChangeRequest request) {
        SysUser user = requireUser(userId);

        // 校验旧密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }

        // 校验两次新密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    /**
     * 学生个人中心概览
     *
     * @param userId 当前用户ID
     * @return 概览数据
     */
    @Override
    public UserCenterOverviewVO getStudentOverview(Long userId) {
        // 获取个人资料
        UserProfileVO profile = getProfile(userId);

        // 统计订单：当前用户作为发布人的订单
        LambdaQueryWrapper<ErrandOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ErrandOrder::getPublisherId, userId);
        List<ErrandOrder> orders = errandOrderMapper.selectList(orderWrapper);

        long unpaid = 0, ongoing = 0, completed = 0, cancelled = 0, appealing = 0;
        for (ErrandOrder order : orders) {
            OrderStatusEnum status = OrderStatusEnum.getByCode(order.getOrderStatus());
            if (status == null) {
                continue;
            }
            switch (status) {
                case UNPAID -> unpaid++;
                case WAITING_ACCEPT, ACCEPTED, CONTACTED, PICKED_UP, DELIVERING, DELIVERED -> ongoing++;
                case COMPLETED -> completed++;
                case CANCELLED, CLOSED -> cancelled++;
                case APPEALING -> appealing++;
            }
        }

        // 统计未读消息数
        LambdaQueryWrapper<StationMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(StationMessage::getReceiverUserId, userId)
                .eq(StationMessage::getIsRead, 0);
        Long unreadCount = stationMessageMapper.selectCount(msgWrapper);

        // 查询跑腿员认证状态
        Integer runnerAuthStatus = null;
        LambdaQueryWrapper<RunnerAuth> authWrapper = new LambdaQueryWrapper<>();
        authWrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getCurrentFlag, 1);
        RunnerAuth auth = runnerAuthMapper.selectOne(authWrapper);
        if (auth != null) {
            runnerAuthStatus = auth.getAuthStatus();
        }

        return UserCenterOverviewVO.builder()
                .profile(profile)
                .totalOrderCount((long) orders.size())
                .unpaidOrderCount(unpaid)
                .ongoingOrderCount(ongoing)
                .completedOrderCount(completed)
                .cancelledOrderCount(cancelled)
                .appealOrderCount(appealing)
                .unreadMessageCount(unreadCount)
                .runnerAuthStatus(runnerAuthStatus)
                .build();
    }

    /**
     * 跑腿员个人中心概览
     *
     * @param userId 当前用户ID
     * @return 概览数据
     */
    @Override
    public RunnerCenterOverviewVO getRunnerOverview(Long userId) {
        // 获取个人资料
        UserProfileVO profile = getProfile(userId);

        // 获取认证信息
        RunnerAuthProfileVO authInfo = getRunnerAuthProfile(userId);

        // 统计接单：当前用户作为跑腿员的订单
        LambdaQueryWrapper<ErrandOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ErrandOrder::getRunnerId, userId);
        List<ErrandOrder> orders = errandOrderMapper.selectList(orderWrapper);

        long ongoing = 0, completed = 0;
        for (ErrandOrder order : orders) {
            OrderStatusEnum status = OrderStatusEnum.getByCode(order.getOrderStatus());
            if (status == null) {
                continue;
            }
            switch (status) {
                case ACCEPTED, CONTACTED, PICKED_UP, DELIVERING, DELIVERED -> ongoing++;
                case COMPLETED -> completed++;
            }
        }

        // 统计收益
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal pendingIncome = BigDecimal.ZERO;
        BigDecimal settledIncome = BigDecimal.ZERO;
        LambdaQueryWrapper<RunnerIncomeRecord> incomeWrapper = new LambdaQueryWrapper<>();
        incomeWrapper.eq(RunnerIncomeRecord::getRunnerId, userId);
        List<RunnerIncomeRecord> incomes = runnerIncomeRecordMapper.selectList(incomeWrapper);
        for (RunnerIncomeRecord record : incomes) {
            BigDecimal amount = record.getIncomeAmount() != null ? record.getIncomeAmount() : BigDecimal.ZERO;
            totalIncome = totalIncome.add(amount);
            SettlementStatusEnum ss = SettlementStatusEnum.getByCode(record.getSettlementStatus());
            if (ss == SettlementStatusEnum.PENDING || ss == SettlementStatusEnum.SETTLING) {
                pendingIncome = pendingIncome.add(amount);
            } else if (ss == SettlementStatusEnum.SETTLED) {
                settledIncome = settledIncome.add(amount);
            }
        }

        // 统计评价
        LambdaQueryWrapper<OrderEvaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(OrderEvaluation::getRunnerId, userId);
        List<OrderEvaluation> evaluations = orderEvaluationMapper.selectList(evalWrapper);
        BigDecimal averageScore = BigDecimal.ZERO;
        if (!evaluations.isEmpty()) {
            BigDecimal totalScore = BigDecimal.ZERO;
            for (OrderEvaluation eval : evaluations) {
                totalScore = totalScore.add(BigDecimal.valueOf(eval.getStarScore()));
            }
            averageScore = totalScore.divide(BigDecimal.valueOf(evaluations.size()), 1, RoundingMode.HALF_UP);
        }

        return RunnerCenterOverviewVO.builder()
                .profile(profile)
                .authInfo(authInfo)
                .acceptedOrderCount((long) orders.size())
                .ongoingOrderCount(ongoing)
                .completedOrderCount(completed)
                .totalIncome(totalIncome)
                .pendingIncome(pendingIncome)
                .settledIncome(settledIncome)
                .averageScore(averageScore)
                .totalEvaluationCount((long) evaluations.size())
                .build();
    }

    /**
     * 查询当前用户的跑腿员认证信息（脱敏）
     *
     * @param userId 当前用户ID
     * @return 认证信息，无认证记录时返回null
     */
    @Override
    public RunnerAuthProfileVO getRunnerAuthProfile(Long userId) {
        LambdaQueryWrapper<RunnerAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getCurrentFlag, 1);
        RunnerAuth auth = runnerAuthMapper.selectOne(wrapper);
        if (auth == null) {
            return null;
        }
        return RunnerAuthProfileVO.from(auth);
    }

    /**
     * 查询用户实体，不存在则抛出异常
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 查询用户角色编码列表
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    private List<String> getUserRoles(Long userId) {
        return sysUserMapper.selectUserRoles(userId).stream()
                .map(SysUserMapper.UserRole::getRoleCode)
                .collect(Collectors.toList());
    }
}
