package com.example.backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.example.backend.service.impl.RefundServiceImpl;
import com.example.backend.vo.RefundRecordVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for refund management service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService unit tests")
class RefundServiceTest {

    /**
     * Initializes MyBatis-Plus table metadata used by lambda wrappers.
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RefundRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PaymentOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ErrandOrder.class);
    }

    /**
     * Refund record mapper mock.
     */
    @Mock
    private RefundRecordMapper refundRecordMapper;

    /**
     * Payment order mapper mock.
     */
    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    /**
     * Errand order mapper mock.
     */
    @Mock
    private ErrandOrderMapper errandOrderMapper;

    /**
     * Service under test.
     */
    @InjectMocks
    private RefundServiceImpl refundService;

    /**
     * List query should return paged refund record VOs.
     */
    @Test
    @DisplayName("should list refund records as paged VOs")
    void shouldListRefundRecords() {
        RefundRecord record = buildRefundRecord(1L, RefundStatusEnum.PENDING.getCode(), new BigDecimal("12.30"));
        record.setRefundNo("RF001");
        record.setApplyUserId(20L);
        record.setRefundType(1);
        record.setRefundReason("cancel");
        record.setRequestId("req-1");
        record.setCreateTime(LocalDateTime.now());

        Page<RefundRecord> recordPage = new Page<>(1, 10, 1);
        recordPage.setRecords(List.of(record));
        when(refundRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(recordPage);

        IPage<RefundRecordVO> result = refundService.list(1, 10, RefundStatusEnum.PENDING.getCode());

        assertEquals(1, result.getTotal(), "total should be preserved");
        assertEquals(1, result.getRecords().size(), "one record should be returned");
        RefundRecordVO vo = result.getRecords().get(0);
        assertEquals(record.getId(), vo.getId(), "id should match");
        assertEquals(record.getOrderId(), vo.getOrderId(), "orderId should match");
        assertEquals(record.getRefundNo(), vo.getRefundNo(), "refundNo should match");
        assertEquals(record.getApplyUserId(), vo.getApplyUserId(), "applyUserId should match");
        assertEquals(record.getRefundType(), vo.getRefundType(), "refundType should match");
        assertEquals(record.getRefundAmount(), vo.getRefundAmount(), "refundAmount should match");
        assertEquals(record.getRefundReason(), vo.getRefundReason(), "refundReason should match");
        assertEquals(record.getRefundStatus(), vo.getRefundStatus(), "refundStatus should match");
        assertEquals(record.getRequestId(), vo.getRequestId(), "requestId should match");
        assertEquals(record.getCreateTime(), vo.getCreateTime(), "createTime should match");
    }

    /**
     * Missing refund record should throw REFUND_NOT_FOUND.
     */
    @Test
    @DisplayName("should throw when refund record does not exist")
    void shouldThrowWhenRefundNotFound() {
        when(refundRecordMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.approve(1L, 99L, buildApproveRequest(RefundStatusEnum.SUCCESS.getCode())));

        assertEquals(ErrorCode.REFUND_NOT_FOUND.getCode(), ex.getCode(), "error code should be REFUND_NOT_FOUND");
        verify(refundRecordMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    /**
     * Terminal refund status should not be approved again.
     */
    @Test
    @DisplayName("should throw when refund status cannot be approved")
    void shouldThrowWhenRefundCannotApprove() {
        RefundRecord record = buildRefundRecord(1L, RefundStatusEnum.SUCCESS.getCode(), new BigDecimal("10.00"));
        when(refundRecordMapper.selectById(1L)).thenReturn(record);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.approve(1L, 99L, buildApproveRequest(RefundStatusEnum.FAILED.getCode())));

        assertEquals(ErrorCode.REFUND_CANNOT_APPROVE.getCode(), ex.getCode(),
                "error code should be REFUND_CANNOT_APPROVE");
        verify(refundRecordMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    /**
     * Zero-row conditional approval update should fail without syncing payment data.
     */
    @Test
    @DisplayName("should throw when conditional approval update affects zero rows")
    void shouldThrowWhenConditionalApprovalUpdateFails() {
        RefundRecord record = buildRefundRecord(1L, RefundStatusEnum.PENDING.getCode(), new BigDecimal("10.00"));
        when(refundRecordMapper.selectById(1L)).thenReturn(record);
        when(refundRecordMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.approve(1L, 99L, buildApproveRequest(RefundStatusEnum.SUCCESS.getCode())));

        assertEquals(ErrorCode.REFUND_CANNOT_APPROVE.getCode(), ex.getCode(),
                "error code should be REFUND_CANNOT_APPROVE");
        verify(paymentOrderMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    /**
     * Failed approval should only update the refund record.
     */
    @Test
    @DisplayName("should only update refund record when approval failed")
    void shouldOnlyUpdateRefundRecordWhenFailed() {
        RefundRecord record = buildRefundRecord(1L, RefundStatusEnum.PENDING.getCode(), new BigDecimal("10.00"));
        when(refundRecordMapper.selectById(1L)).thenReturn(record);
        when(refundRecordMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        refundService.approve(1L, 99L, buildApproveRequest(RefundStatusEnum.FAILED.getCode()));

        verify(refundRecordMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(paymentOrderMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
        verify(errandOrderMapper, never()).selectById(any());
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    /**
     * Successful refund reaching full amount should mark records as refunded.
     */
    @Test
    @DisplayName("should set refunded when successful refund reaches full amount")
    void shouldSetRefundedWhenSuccessfulFullRefund() {
        RefundRecord record = buildRefundRecord(1L, RefundStatusEnum.PROCESSING.getCode(), new BigDecimal("30.00"));
        PaymentOrder paymentOrder = buildPaymentOrder(50L, new BigDecimal("70.00"));
        ErrandOrder errandOrder = buildErrandOrder(50L, new BigDecimal("100.00"));
        when(refundRecordMapper.selectById(1L)).thenReturn(record);
        when(refundRecordMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentOrder);
        when(errandOrderMapper.selectById(50L)).thenReturn(errandOrder);

        refundService.approve(1L, 99L, buildApproveRequest(RefundStatusEnum.SUCCESS.getCode()));

        assertEquals(new BigDecimal("100.00"), paymentOrder.getRefundAmount(), "total refund amount should update");
        assertEquals(PayStatusEnum.REFUNDED.getCode(), paymentOrder.getPayStatus(),
                "payment order should be refunded");
        assertEquals(PayStatusEnum.REFUNDED.getCode(), errandOrder.getPayStatus(),
                "errand order should be refunded");
        verify(refundRecordMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(paymentOrderMapper).updateById(paymentOrder);
        verify(errandOrderMapper).updateById(errandOrder);
    }

    /**
     * Successful refund below full amount should mark records as partial refund.
     */
    @Test
    @DisplayName("should set partial refund when successful refund is below full amount")
    void shouldSetPartialRefundWhenSuccessfulPartialRefund() {
        RefundRecord record = buildRefundRecord(1L, RefundStatusEnum.PENDING.getCode(), new BigDecimal("20.00"));
        PaymentOrder paymentOrder = buildPaymentOrder(50L, new BigDecimal("10.00"));
        ErrandOrder errandOrder = buildErrandOrder(50L, new BigDecimal("100.00"));
        when(refundRecordMapper.selectById(1L)).thenReturn(record);
        when(refundRecordMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentOrder);
        when(errandOrderMapper.selectById(50L)).thenReturn(errandOrder);

        refundService.approve(1L, 99L, buildApproveRequest(RefundStatusEnum.SUCCESS.getCode()));

        assertEquals(new BigDecimal("30.00"), paymentOrder.getRefundAmount(), "total refund amount should update");
        assertEquals(PayStatusEnum.PARTIAL_REFUND.getCode(), paymentOrder.getPayStatus(),
                "payment order should be partial refund");
        assertEquals(PayStatusEnum.PARTIAL_REFUND.getCode(), errandOrder.getPayStatus(),
                "errand order should be partial refund");
        verify(refundRecordMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(paymentOrderMapper).updateById(paymentOrder);
        verify(errandOrderMapper).updateById(errandOrder);
    }

    /**
     * Builds a refund record test fixture.
     */
    private RefundRecord buildRefundRecord(Long id, Integer status, BigDecimal amount) {
        RefundRecord record = new RefundRecord();
        record.setId(id);
        record.setOrderId(50L);
        record.setRefundStatus(status);
        record.setRefundAmount(amount);
        return record;
    }

    /**
     * Builds an approval request test fixture.
     */
    private RefundApproveRequest buildApproveRequest(Integer status) {
        RefundApproveRequest request = new RefundApproveRequest();
        request.setRefundStatus(status);
        request.setApproveResult("ok");
        return request;
    }

    /**
     * Builds a payment order test fixture.
     */
    private PaymentOrder buildPaymentOrder(Long orderId, BigDecimal refundAmount) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(2L);
        paymentOrder.setOrderId(orderId);
        paymentOrder.setRefundAmount(refundAmount);
        paymentOrder.setPayStatus(PayStatusEnum.PAID.getCode());
        return paymentOrder;
    }

    /**
     * Builds an errand order test fixture.
     */
    private ErrandOrder buildErrandOrder(Long id, BigDecimal orderAmount) {
        ErrandOrder errandOrder = new ErrandOrder();
        errandOrder.setId(id);
        errandOrder.setOrderAmount(orderAmount);
        errandOrder.setPayStatus(PayStatusEnum.PAID.getCode());
        return errandOrder;
    }
}
