package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.enums.AppealStatusEnum;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.enums.RefundStatusEnum;
import com.example.backend.common.enums.RoleCodeEnum;
import com.example.backend.entity.AppealRecord;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.RefundRecord;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.SysUserRole;
import com.example.backend.mapper.AppealRecordMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.mapper.RefundRecordMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.SysUserRoleMapper;
import com.example.backend.service.AdminStatService;
import com.example.backend.vo.AdminOverviewVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 管理员统计服务实现类
 * <p>
 * 通过汇总各业务表的查询结果，生成管理端首页所需的统计概览数据。
 * 所有金额统计均使用 {@link BigDecimal} 并防止 NPE。
 * </p>
 */
@Service
public class AdminStatServiceImpl implements AdminStatService {

    private final ErrandOrderMapper errandOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final RunnerAuthMapper runnerAuthMapper;
    private final AppealRecordMapper appealRecordMapper;

    /**
     * 构造函数注入各业务Mapper
     */
    public AdminStatServiceImpl(ErrandOrderMapper errandOrderMapper,
                                 PaymentOrderMapper paymentOrderMapper,
                                 RefundRecordMapper refundRecordMapper,
                                 SysUserRoleMapper sysUserRoleMapper,
                                 RunnerAuthMapper runnerAuthMapper,
                                 AppealRecordMapper appealRecordMapper) {
        this.errandOrderMapper = errandOrderMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.runnerAuthMapper = runnerAuthMapper;
        this.appealRecordMapper = appealRecordMapper;
    }

    /**
     * 查询后台统计概览数据
     * <p>
     * 分别统计订单、支付金额、退款金额、跑腿员用户、待审核认证、待处理申诉和待处理退款。
     * 所有统计均使用 MyBatis-Plus 的 LambdaQueryWrapper 构造条件查询，
     * 金额查询使用 Mapper 聚合 SUM，并对 SUM 返回 null 做 BigDecimal.ZERO 兜底。
     * </p>
     *
     * @return 统计概览视图对象，所有数值字段含默认值
     */
    @Override
    public AdminOverviewVO getOverview() {
        // 统计订单总数：MyBatis-Plus 自动过滤逻辑删除（is_deleted=0）
        Long totalOrderCount = errandOrderMapper.selectCount(null);

        // 统计今日订单数：创建时间为今天的未删除订单
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<ErrandOrder> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(ErrandOrder::getCreateTime, todayStart);
        Long todayOrderCount = errandOrderMapper.selectCount(todayWrapper);

        // 统计已支付金额：数据库端SUM，避免加载全部流水到内存
        BigDecimal paidAmount = paymentOrderMapper.sumPayAmountByStatus(PayStatusEnum.PAID.getCode());

        // 统计退款成功金额：数据库端SUM
        BigDecimal refundAmount = refundRecordMapper.sumRefundAmountByStatus(RefundStatusEnum.SUCCESS.getCode());

        // 统计有效跑腿员数：role_code=RUNNER 且 role_status=1（有效）
        LambdaQueryWrapper<SysUserRole> runnerWrapper = new LambdaQueryWrapper<>();
        runnerWrapper.eq(SysUserRole::getRoleCode, RoleCodeEnum.RUNNER.getCode())
                .eq(SysUserRole::getRoleStatus, 1); // 1=有效状态
        Long activeRunnerCount = sysUserRoleMapper.selectCount(runnerWrapper);

        // 统计待审核跑腿员认证数：auth_status=0（待审核）
        LambdaQueryWrapper<RunnerAuth> authWrapper = new LambdaQueryWrapper<>();
        authWrapper.eq(RunnerAuth::getAuthStatus, AuthStatusEnum.PENDING.getCode());
        Long pendingRunnerAuthCount = runnerAuthMapper.selectCount(authWrapper);

        // 统计待处理申诉数：appeal_status 为 0（待处理）或 1（处理中）
        LambdaQueryWrapper<AppealRecord> appealWrapper = new LambdaQueryWrapper<>();
        appealWrapper.in(AppealRecord::getAppealStatus,
                AppealStatusEnum.PENDING.getCode(), AppealStatusEnum.PROCESSING.getCode());
        Long pendingAppealCount = appealRecordMapper.selectCount(appealWrapper);

        // 统计待处理退款数：refund_status 为 0（待处理）或 1（处理中）
        LambdaQueryWrapper<RefundRecord> pendingRefundWrapper = new LambdaQueryWrapper<>();
        pendingRefundWrapper.in(RefundRecord::getRefundStatus,
                RefundStatusEnum.PENDING.getCode(), RefundStatusEnum.PROCESSING.getCode());
        Long pendingRefundCount = refundRecordMapper.selectCount(pendingRefundWrapper);

        return AdminOverviewVO.builder()
                .totalOrderCount(defaultZero(totalOrderCount))
                .todayOrderCount(defaultZero(todayOrderCount))
                .paidAmount(defaultZero(paidAmount))
                .refundAmount(defaultZero(refundAmount))
                .activeRunnerCount(defaultZero(activeRunnerCount))
                .pendingRunnerAuthCount(defaultZero(pendingRunnerAuthCount))
                .pendingAppealCount(defaultZero(pendingAppealCount))
                .pendingRefundCount(defaultZero(pendingRefundCount))
                .build();
    }

    /**
     * 数值型统计结果防 NPE 兜底
     * <p>
     * 当查询结果为 null 时返回 0，确保接口返回值不含 null。
     * </p>
     *
     * @param value 原始查询结果（可能为null）
     * @return 非null的数值
     */
    private Long defaultZero(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * 金额型统计结果防 NPE 兜底
     * <p>
     * 当查询结果为 null 时返回 BigDecimal.ZERO。
     * </p>
     *
     * @param value 原始查询结果（可能为null）
     * @return 非null的金额
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
