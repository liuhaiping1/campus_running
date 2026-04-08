package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.SettlementStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.RunnerIncomeRecord;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.RunnerIncomeRecordMapper;
import com.example.backend.service.RunnerIncomeService;
import com.example.backend.vo.RunnerIncomeOverviewVO;
import com.example.backend.vo.RunnerIncomeRecordVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 跑腿员收益查询服务实现。
 */
@Service
public class RunnerIncomeServiceImpl implements RunnerIncomeService {

    /**
     * 跑腿员收益记录 Mapper。
     */
    private final RunnerIncomeRecordMapper runnerIncomeRecordMapper;

    /**
     * 跑腿员认证 Mapper。
     */
    private final RunnerAuthMapper runnerAuthMapper;

    /**
     * 构造函数注入依赖。
     *
     * @param runnerIncomeRecordMapper 跑腿员收益记录 Mapper
     * @param runnerAuthMapper         跑腿员认证 Mapper
     */
    public RunnerIncomeServiceImpl(RunnerIncomeRecordMapper runnerIncomeRecordMapper,
                                   RunnerAuthMapper runnerAuthMapper) {
        this.runnerIncomeRecordMapper = runnerIncomeRecordMapper;
        this.runnerAuthMapper = runnerAuthMapper;
    }

    /**
     * 查询当前跑腿员收益总览。
     *
     * @param userId 当前登录用户ID
     * @return 收益总览
     */
    @Override
    public RunnerIncomeOverviewVO overview(Long userId) {
        Long runnerId = resolveRunnerId(userId);
        LambdaQueryWrapper<RunnerIncomeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunnerIncomeRecord::getRunnerId, runnerId)
                .eq(RunnerIncomeRecord::getIsDeleted, 0);
        List<RunnerIncomeRecord> records = runnerIncomeRecordMapper.selectList(wrapper);

        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime nextMonthStart = firstDay.plusMonths(1).atStartOfDay();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal pendingIncome = BigDecimal.ZERO;
        BigDecimal settledIncome = BigDecimal.ZERO;
        BigDecimal settlingIncome = BigDecimal.ZERO;
        BigDecimal rolledBackIncome = BigDecimal.ZERO;
        BigDecimal currentMonthIncome = BigDecimal.ZERO;

        for (RunnerIncomeRecord record : records) {
            BigDecimal incomeAmount = zeroIfNull(record.getIncomeAmount());
            totalIncome = totalIncome.add(incomeAmount);

            if (SettlementStatusEnum.PENDING.getCode().equals(record.getSettlementStatus())) {
                pendingIncome = pendingIncome.add(incomeAmount);
            } else if (SettlementStatusEnum.SETTLED.getCode().equals(record.getSettlementStatus())) {
                settledIncome = settledIncome.add(incomeAmount);
            } else if (SettlementStatusEnum.SETTLING.getCode().equals(record.getSettlementStatus())) {
                settlingIncome = settlingIncome.add(incomeAmount);
            }

            rolledBackIncome = rolledBackIncome.add(zeroIfNull(record.getRollbackAmount()));
            if (isCurrentMonth(record.getCreateTime(), monthStart, nextMonthStart)) {
                currentMonthIncome = currentMonthIncome.add(incomeAmount);
            }
        }

        return RunnerIncomeOverviewVO.builder()
                .totalIncome(totalIncome)
                .pendingIncome(pendingIncome)
                .settledIncome(settledIncome)
                .settlingIncome(settlingIncome)
                .rolledBackIncome(rolledBackIncome)
                .currentMonthIncome(currentMonthIncome)
                .totalOrderCount((long) records.size())
                .build();
    }

    /**
     * 分页查询当前跑腿员收益明细。
     *
     * @param userId           当前登录用户ID
     * @param page             页码
     * @param size             每页条数
     * @param settlementStatus 结算状态筛选，null 表示全部
     * @return 收益明细分页
     */
    @Override
    public IPage<RunnerIncomeRecordVO> list(Long userId, int page, int size, Integer settlementStatus) {
        validatePage(page, size);
        validateSettlementStatus(settlementStatus);
        Long runnerId = resolveRunnerId(userId);
        Page<RunnerIncomeRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RunnerIncomeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunnerIncomeRecord::getRunnerId, runnerId)
                .eq(RunnerIncomeRecord::getIsDeleted, 0);
        if (settlementStatus != null) {
            wrapper.eq(RunnerIncomeRecord::getSettlementStatus, settlementStatus);
        }
        wrapper.orderByDesc(RunnerIncomeRecord::getCreateTime);

        Page<RunnerIncomeRecord> recordPage = runnerIncomeRecordMapper.selectPage(pageParam, wrapper);
        List<RunnerIncomeRecordVO> voList = recordPage.getRecords().stream()
                .map(this::buildRecordVO)
                .collect(Collectors.toList());

        Page<RunnerIncomeRecordVO> resultPage =
                new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据当前登录用户解析跑腿员用户ID。
     *
     * @param userId 当前登录用户ID
     * @return 跑腿员用户ID
     */
    private Long resolveRunnerId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<RunnerAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getAuthStatus, AuthStatusEnum.APPROVED.getCode())
                .eq(RunnerAuth::getCurrentFlag, 1);
        RunnerAuth runnerAuth = runnerAuthMapper.selectOne(wrapper);
        if (runnerAuth == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return runnerAuth.getUserId();
    }

    /**
     * 校验分页参数。
     *
     * @param page 页码
     * @param size 每页条数
     */
    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 校验结算状态参数。
     *
     * @param settlementStatus 结算状态
     */
    private void validateSettlementStatus(Integer settlementStatus) {
        if (settlementStatus != null && SettlementStatusEnum.getByCode(settlementStatus) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 构造收益明细 VO。
     *
     * @param record 收益记录实体
     * @return 收益明细 VO
     */
    private RunnerIncomeRecordVO buildRecordVO(RunnerIncomeRecord record) {
        return RunnerIncomeRecordVO.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .runnerId(record.getRunnerId())
                .incomeAmount(record.getIncomeAmount())
                .commissionAmount(record.getCommissionAmount())
                .settlementStatus(record.getSettlementStatus())
                .settlementTime(record.getSettlementTime())
                .rollbackAmount(record.getRollbackAmount())
                .rollbackReason(record.getRollbackReason())
                .createTime(record.getCreateTime())
                .build();
    }

    /**
     * 判断记录创建时间是否位于本月。
     *
     * @param createTime     创建时间
     * @param monthStart     本月开始时间
     * @param nextMonthStart 下月开始时间
     * @return 是否本月
     */
    private boolean isCurrentMonth(LocalDateTime createTime, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        return createTime != null && !createTime.isBefore(monthStart) && createTime.isBefore(nextMonthStart);
    }

    /**
     * 空金额转换为零。
     *
     * @param amount 原金额
     * @return 非空金额
     */
    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
