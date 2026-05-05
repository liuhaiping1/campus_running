package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.enums.RefundStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.RefundApproveRequest;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.PaymentOrder;
import com.example.backend.entity.RefundRecord;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.mapper.RefundRecordMapper;
import com.example.backend.service.RefundService;
import com.example.backend.vo.RefundRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Refund management service implementation.
 */
@Service
public class RefundServiceImpl implements RefundService {

    private final RefundRecordMapper refundRecordMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final ErrandOrderMapper errandOrderMapper;

    /**
     * Creates a refund service with required mappers.
     *
     * @param refundRecordMapper refund record mapper
     * @param paymentOrderMapper payment order mapper
     * @param errandOrderMapper  errand order mapper
     */
    public RefundServiceImpl(RefundRecordMapper refundRecordMapper,
                             PaymentOrderMapper paymentOrderMapper,
                             ErrandOrderMapper errandOrderMapper) {
        this.refundRecordMapper = refundRecordMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.errandOrderMapper = errandOrderMapper;
    }

    /**
     * Lists refund records by status and creation time.
     *
     * @param page         page number starting from 1
     * @param size         page size
     * @param refundStatus optional refund status filter
     * @return paged refund record view objects
     */
    @Override
    public IPage<RefundRecordVO> list(int page, int size, Integer refundStatus) {
        Page<RefundRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        if (refundStatus != null) {
            wrapper.eq(RefundRecord::getRefundStatus, refundStatus);
        }
        wrapper.orderByDesc(RefundRecord::getCreateTime);

        Page<RefundRecord> recordPage = refundRecordMapper.selectPage(pageParam, wrapper);
        List<RefundRecordVO> records = recordPage.getRecords().stream()
                .map(this::buildRefundRecordVO)
                .collect(Collectors.toList());

        Page<RefundRecordVO> resultPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * Approves a pending or processing refund.
     *
     * @param id      refund record id
     * @param adminId approval admin id
     * @param request approval request
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long adminId, RefundApproveRequest request) {
        RefundRecord refundRecord = refundRecordMapper.selectById(id);
        if (refundRecord == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        if (!isFinalApprovalStatus(request.getRefundStatus())) {
            throw new BusinessException(ErrorCode.REFUND_CANNOT_APPROVE);
        }
        if (!canApprove(refundRecord.getRefundStatus())) {
            throw new BusinessException(ErrorCode.REFUND_CANNOT_APPROVE);
        }

        LocalDateTime refundTime = RefundStatusEnum.SUCCESS.getCode().equals(request.getRefundStatus())
                ? LocalDateTime.now()
                : null;
        updateRefundRecordStatus(id, adminId, request, refundTime);
        if (RefundStatusEnum.SUCCESS.getCode().equals(request.getRefundStatus())) {
            syncPaymentRefundStatus(refundRecord);
        }
    }

    /**
     * Checks whether the requested approval status is terminal.
     *
     * @param refundStatus requested refund status
     * @return true if success or failed
     */
    private boolean isFinalApprovalStatus(Integer refundStatus) {
        return RefundStatusEnum.SUCCESS.getCode().equals(refundStatus)
                || RefundStatusEnum.FAILED.getCode().equals(refundStatus);
    }

    /**
     * Checks whether a refund record can be approved.
     *
     * @param refundStatus current refund status
     * @return true if pending or processing
     */
    private boolean canApprove(Integer refundStatus) {
        return RefundStatusEnum.PENDING.getCode().equals(refundStatus)
                || RefundStatusEnum.PROCESSING.getCode().equals(refundStatus);
    }

    /**
     * Updates refund approval status with a status condition.
     *
     * @param id         refund record id
     * @param adminId    approval admin id
     * @param request    approval request
     * @param refundTime refund success time, null for failed approval
     */
    private void updateRefundRecordStatus(Long id, Long adminId, RefundApproveRequest request, LocalDateTime refundTime) {
        LambdaUpdateWrapper<RefundRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RefundRecord::getId, id)
                .in(RefundRecord::getRefundStatus,
                        RefundStatusEnum.PENDING.getCode(), RefundStatusEnum.PROCESSING.getCode())
                .set(RefundRecord::getRefundStatus, request.getRefundStatus())
                .set(RefundRecord::getApproveAdminId, adminId)
                .set(RefundRecord::getApproveResult, request.getApproveResult())
                .set(RefundRecord::getUpdateTime, LocalDateTime.now());
        if (refundTime != null) {
            wrapper.set(RefundRecord::getRefundTime, refundTime);
        }
        int rows = refundRecordMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.REFUND_CANNOT_APPROVE);
        }
    }

    /**
     * Synchronizes payment order and errand order refund status.
     *
     * @param refundRecord approved refund record
     */
    private void syncPaymentRefundStatus(RefundRecord refundRecord) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getOrderId, refundRecord.getOrderId());
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(wrapper);
        if (paymentOrder == null) {
            return;
        }

        BigDecimal originalRefundAmount = defaultZero(paymentOrder.getRefundAmount());
        BigDecimal currentRefundAmount = defaultZero(refundRecord.getRefundAmount());
        BigDecimal totalRefundAmount = originalRefundAmount.add(currentRefundAmount);
        paymentOrder.setRefundAmount(totalRefundAmount);

        ErrandOrder errandOrder = errandOrderMapper.selectById(refundRecord.getOrderId());
        if (errandOrder != null && errandOrder.getOrderAmount() != null) {
            Integer payStatus = totalRefundAmount.compareTo(errandOrder.getOrderAmount()) >= 0
                    ? PayStatusEnum.REFUNDED.getCode()
                    : PayStatusEnum.PARTIAL_REFUND.getCode();
            paymentOrder.setPayStatus(payStatus);
            errandOrder.setPayStatus(payStatus);
            errandOrder.setUpdateTime(LocalDateTime.now());
            errandOrderMapper.updateById(errandOrder);
        }

        paymentOrder.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.updateById(paymentOrder);
    }

    /**
     * Converts null amount to zero.
     *
     * @param amount source amount
     * @return non-null amount
     */
    private BigDecimal defaultZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * Builds refund record view object.
     *
     * @param record refund record entity
     * @return refund record view object
     */
    private RefundRecordVO buildRefundRecordVO(RefundRecord record) {
        return RefundRecordVO.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .payNo(record.getPayNo())
                .refundNo(record.getRefundNo())
                .applyUserId(record.getApplyUserId())
                .refundType(record.getRefundType())
                .refundAmount(record.getRefundAmount())
                .refundReason(record.getRefundReason())
                .refundStatus(record.getRefundStatus())
                .requestId(record.getRequestId())
                .approveAdminId(record.getApproveAdminId())
                .approveResult(record.getApproveResult())
                .refundTime(record.getRefundTime())
                .createTime(record.getCreateTime())
                .build();
    }
}
