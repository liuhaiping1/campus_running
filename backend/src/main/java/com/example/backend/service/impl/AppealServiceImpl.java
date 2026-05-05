package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AppealStatusEnum;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.AppealHandleRequest;
import com.example.backend.dto.request.AppealSubmitRequest;
import com.example.backend.entity.AppealRecord;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.mapper.AppealRecordMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.service.AppealService;
import com.example.backend.vo.AppealRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for appeal submission and admin handling.
 */
@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    /**
     * Appeal record mapper.
     */
    private final AppealRecordMapper appealRecordMapper;

    /**
     * Errand order mapper.
     */
    private final ErrandOrderMapper errandOrderMapper;

    /**
     * Submits an appeal for an order.
     *
     * @param userId current user ID
     * @param request appeal submit request
     * @return created appeal ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, AppealSubmitRequest request) {
        ErrandOrder order = errandOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        String applyRole = resolveApplyRole(order, userId);
        if (applyRole == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }

        LambdaQueryWrapper<AppealRecord> activeAppealWrapper = new LambdaQueryWrapper<>();
        activeAppealWrapper.eq(AppealRecord::getOrderId, request.getOrderId())
                .in(AppealRecord::getAppealStatus,
                        AppealStatusEnum.PENDING.getCode(), AppealStatusEnum.PROCESSING.getCode())
                .eq(AppealRecord::getIsDeleted, 0);
        if (appealRecordMapper.selectCount(activeAppealWrapper) > 0) {
            throw new BusinessException(ErrorCode.APPEAL_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        Integer beforeOrderStatus = order.getOrderStatus();
        AppealRecord appeal = new AppealRecord();
        appeal.setOrderId(request.getOrderId());
        appeal.setAppealNo("AP" + IdWorker.getIdStr());
        appeal.setApplyUserId(userId);
        appeal.setApplyRole(applyRole);
        appeal.setAppealType(request.getAppealType());
        appeal.setAppealContent(request.getAppealContent());
        appeal.setEvidenceUrls(request.getEvidenceUrls());
        appeal.setAppealStatus(AppealStatusEnum.PENDING.getCode());
        appeal.setBeforeOrderStatus(beforeOrderStatus);
        appeal.setCreateTime(now);
        appeal.setUpdateTime(now);
        appealRecordMapper.insert(appeal);

        ErrandOrder updateOrder = new ErrandOrder();
        updateOrder.setAppealFlag(1);
        updateOrder.setOrderStatus(OrderStatusEnum.APPEALING.getCode());
        updateOrder.setUpdateTime(now);
        LambdaUpdateWrapper<ErrandOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ErrandOrder::getId, order.getId())
                .eq(ErrandOrder::getOrderStatus, beforeOrderStatus);
        int updatedRows = errandOrderMapper.update(updateOrder, updateWrapper);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }

        return appeal.getId();
    }

    /**
     * Lists appeal records for the admin console.
     *
     * @param page page number starting at 1
     * @param size page size
     * @param appealStatus optional appeal status filter
     * @return paged appeal records
     */
    @Override
    public IPage<AppealRecordVO> list(int page, int size, Integer appealStatus) {
        Page<AppealRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AppealRecord> wrapper = new LambdaQueryWrapper<>();
        if (appealStatus != null) {
            wrapper.eq(AppealRecord::getAppealStatus, appealStatus);
        }
        wrapper.orderByDesc(AppealRecord::getCreateTime);

        Page<AppealRecord> appealPage = appealRecordMapper.selectPage(pageParam, wrapper);
        List<AppealRecordVO> records = appealPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<AppealRecordVO> resultPage = new Page<>(appealPage.getCurrent(), appealPage.getSize(), appealPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * Handles an appeal in the admin console.
     *
     * @param id appeal ID
     * @param adminId current admin ID
     * @param request appeal handle request
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Long adminId, AppealHandleRequest request) {
        if (!isAllowedHandleStatus(request.getAppealStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (request.getResultOrderStatus() != null
                && OrderStatusEnum.getByCode(request.getResultOrderStatus()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        AppealRecord appeal = appealRecordMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException(ErrorCode.APPEAL_NOT_FOUND);
        }
        if (!isHandleableStatus(appeal.getAppealStatus())) {
            throw new BusinessException(ErrorCode.APPEAL_CANNOT_HANDLE);
        }

        appeal.setAppealStatus(request.getAppealStatus());
        appeal.setResultOrderStatus(request.getResultOrderStatus());
        appeal.setResponsibilityType(request.getResponsibilityType());
        appeal.setRefundDecision(request.getRefundDecision());
        appeal.setHandleAdminId(adminId);
        appeal.setHandleResult(request.getHandleResult());
        LocalDateTime now = LocalDateTime.now();
        appeal.setHandleTime(now);
        appeal.setUpdateTime(now);
        LambdaUpdateWrapper<AppealRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AppealRecord::getId, id)
                .in(AppealRecord::getAppealStatus,
                        AppealStatusEnum.PENDING.getCode(), AppealStatusEnum.PROCESSING.getCode());
        int rows = appealRecordMapper.update(appeal, updateWrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.APPEAL_CANNOT_HANDLE);
        }

        ErrandOrder order = new ErrandOrder();
        order.setId(appeal.getOrderId());
        order.setAppealFlag(0);
        order.setOrderStatus(request.getResultOrderStatus() != null
                ? request.getResultOrderStatus()
                : appeal.getBeforeOrderStatus());
        order.setUpdateTime(now);
        errandOrderMapper.updateById(order);
    }

    /**
     * Resolves the applicant role for the current user.
     *
     * @param order appealed order
     * @param userId current user ID
     * @return STUDENT, RUNNER, or null when user is not a participant
     */
    private String resolveApplyRole(ErrandOrder order, Long userId) {
        if (order.getPublisherId() != null && order.getPublisherId().equals(userId)) {
            return "STUDENT";
        }
        if (order.getRunnerId() != null && order.getRunnerId().equals(userId)) {
            return "RUNNER";
        }
        return null;
    }

    /**
     * Checks whether a stored appeal status can be handled.
     *
     * @param appealStatus stored appeal status
     * @return true when status is pending or processing
     */
    private boolean isHandleableStatus(Integer appealStatus) {
        return AppealStatusEnum.PENDING.getCode().equals(appealStatus)
                || AppealStatusEnum.PROCESSING.getCode().equals(appealStatus);
    }

    /**
     * Checks whether a requested handle status is allowed.
     *
     * @param appealStatus requested final appeal status
     * @return true when status is upheld, rejected, or closed
     */
    private boolean isAllowedHandleStatus(Integer appealStatus) {
        return AppealStatusEnum.UPHELD.getCode().equals(appealStatus)
                || AppealStatusEnum.REJECTED.getCode().equals(appealStatus)
                || AppealStatusEnum.CLOSED.getCode().equals(appealStatus);
    }

    /**
     * Converts an appeal entity to a VO.
     *
     * @param appeal appeal entity
     * @return appeal VO
     */
    private AppealRecordVO toVO(AppealRecord appeal) {
        return AppealRecordVO.builder()
                .id(appeal.getId())
                .orderId(appeal.getOrderId())
                .appealNo(appeal.getAppealNo())
                .applyUserId(appeal.getApplyUserId())
                .applyRole(appeal.getApplyRole())
                .appealType(appeal.getAppealType())
                .appealContent(appeal.getAppealContent())
                .evidenceUrls(appeal.getEvidenceUrls())
                .appealStatus(appeal.getAppealStatus())
                .beforeOrderStatus(appeal.getBeforeOrderStatus())
                .resultOrderStatus(appeal.getResultOrderStatus())
                .responsibilityType(appeal.getResponsibilityType())
                .refundDecision(appeal.getRefundDecision())
                .handleAdminId(appeal.getHandleAdminId())
                .handleResult(appeal.getHandleResult())
                .handleTime(appeal.getHandleTime())
                .createTime(appeal.getCreateTime())
                .updateTime(appeal.getUpdateTime())
                .build();
    }
}
