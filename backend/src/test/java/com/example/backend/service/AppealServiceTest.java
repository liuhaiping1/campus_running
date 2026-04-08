package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.example.backend.service.impl.AppealServiceImpl;
import com.example.backend.vo.AppealRecordVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for appeal submission and admin handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppealService unit tests")
class AppealServiceTest {

    /**
     * Mock appeal record mapper.
     */
    @Mock
    private AppealRecordMapper appealRecordMapper;

    /**
     * Mock errand order mapper.
     */
    @Mock
    private ErrandOrderMapper errandOrderMapper;

    /**
     * Service under test.
     */
    @InjectMocks
    private AppealServiceImpl appealService;

    /**
     * Submitting an appeal by the publisher creates the appeal and moves the order to appealing.
     */
    @Test
    @DisplayName("submit succeeds for order participant")
    void shouldSubmitAppealSuccessfully() {
        Long userId = 11L;
        ErrandOrder order = buildOrder(100L, userId, 22L, OrderStatusEnum.ACCEPTED.getCode());
        AppealSubmitRequest request = buildSubmitRequest(order.getId());
        when(errandOrderMapper.selectById(order.getId())).thenReturn(order);
        when(appealRecordMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            AppealRecord appeal = invocation.getArgument(0);
            appeal.setId(200L);
            return 1;
        }).when(appealRecordMapper).insert(any(AppealRecord.class));
        when(errandOrderMapper.update(any(ErrandOrder.class), any(Wrapper.class))).thenReturn(1);

        Long appealId = appealService.submit(userId, request);

        assertEquals(200L, appealId);
        ArgumentCaptor<AppealRecord> appealCaptor = ArgumentCaptor.forClass(AppealRecord.class);
        verify(appealRecordMapper).insert(appealCaptor.capture());
        AppealRecord savedAppeal = appealCaptor.getValue();
        assertEquals(order.getId(), savedAppeal.getOrderId());
        assertTrue(savedAppeal.getAppealNo().startsWith("AP"));
        assertEquals(userId, savedAppeal.getApplyUserId());
        assertEquals("STUDENT", savedAppeal.getApplyRole());
        assertEquals(AppealStatusEnum.PENDING.getCode(), savedAppeal.getAppealStatus());
        assertEquals(order.getOrderStatus(), savedAppeal.getBeforeOrderStatus());

        ArgumentCaptor<ErrandOrder> orderCaptor = ArgumentCaptor.forClass(ErrandOrder.class);
        verify(errandOrderMapper).update(orderCaptor.capture(), any(Wrapper.class));
        ErrandOrder updatedOrder = orderCaptor.getValue();
        assertEquals(1, updatedOrder.getAppealFlag());
        assertEquals(OrderStatusEnum.APPEALING.getCode(), updatedOrder.getOrderStatus());
    }

    /**
     * Users outside the order publisher and runner cannot submit appeals.
     */
    @Test
    @DisplayName("submit fails for non participant")
    void shouldRejectAppealFromNonParticipant() {
        ErrandOrder order = buildOrder(100L, 11L, 22L, OrderStatusEnum.ACCEPTED.getCode());
        when(errandOrderMapper.selectById(order.getId())).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submit(99L, buildSubmitRequest(order.getId())));

        assertEquals(ErrorCode.ORDER_NOT_OWNED.getCode(), exception.getCode());
        verify(appealRecordMapper, never()).insert(any(AppealRecord.class));
    }

    /**
     * An order with a pending or processing appeal cannot receive another appeal.
     */
    @Test
    @DisplayName("submit fails when active appeal exists")
    void shouldRejectDuplicateActiveAppeal() {
        Long userId = 11L;
        ErrandOrder order = buildOrder(100L, userId, 22L, OrderStatusEnum.ACCEPTED.getCode());
        when(errandOrderMapper.selectById(order.getId())).thenReturn(order);
        when(appealRecordMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submit(userId, buildSubmitRequest(order.getId())));

        assertEquals(ErrorCode.APPEAL_EXISTS.getCode(), exception.getCode());
        verify(appealRecordMapper, never()).insert(any(AppealRecord.class));
    }

    /**
     * A zero-row conditional order update is treated as an order status conflict.
     */
    @Test
    @DisplayName("submit fails when conditional order update affects zero rows")
    void shouldRejectWhenSubmitOrderUpdateConflicts() {
        Long userId = 11L;
        ErrandOrder order = buildOrder(100L, userId, 22L, OrderStatusEnum.ACCEPTED.getCode());
        when(errandOrderMapper.selectById(order.getId())).thenReturn(order);
        when(appealRecordMapper.selectCount(any())).thenReturn(0L);
        when(appealRecordMapper.insert(any(AppealRecord.class))).thenReturn(1);
        when(errandOrderMapper.update(any(ErrandOrder.class), any(Wrapper.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submit(userId, buildSubmitRequest(order.getId())));

        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode());
    }

    /**
     * Admin list returns appeal VOs in a paged result.
     */
    @Test
    @DisplayName("list succeeds")
    void shouldListAppealsSuccessfully() {
        AppealRecord appeal = buildAppeal(200L, AppealStatusEnum.PENDING.getCode());
        Page<AppealRecord> mapperPage = new Page<>(1, 10, 1);
        mapperPage.setRecords(Collections.singletonList(appeal));
        when(appealRecordMapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

        IPage<AppealRecordVO> result = appealService.list(1, 10, AppealStatusEnum.PENDING.getCode());

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        AppealRecordVO vo = result.getRecords().get(0);
        assertEquals(appeal.getId(), vo.getId());
        assertEquals(appeal.getAppealNo(), vo.getAppealNo());
        assertEquals(appeal.getAppealStatus(), vo.getAppealStatus());
    }

    /**
     * Handling an appeal records admin result and restores or sets the order status.
     */
    @Test
    @DisplayName("handle succeeds and syncs order status")
    void shouldHandleAppealSuccessfully() {
        Long adminId = 9L;
        AppealRecord appealToRestore = buildAppeal(200L, AppealStatusEnum.PENDING.getCode());
        when(appealRecordMapper.selectById(appealToRestore.getId())).thenReturn(appealToRestore);
        when(appealRecordMapper.update(any(AppealRecord.class), any(Wrapper.class))).thenReturn(1);
        when(errandOrderMapper.updateById(any(ErrandOrder.class))).thenReturn(1);

        AppealHandleRequest restoreRequest = buildHandleRequest(AppealStatusEnum.REJECTED.getCode(), null);
        appealService.handle(appealToRestore.getId(), adminId, restoreRequest);

        ArgumentCaptor<AppealRecord> appealCaptor = ArgumentCaptor.forClass(AppealRecord.class);
        verify(appealRecordMapper).update(appealCaptor.capture(), any(Wrapper.class));
        AppealRecord handledAppeal = appealCaptor.getValue();
        assertEquals(AppealStatusEnum.REJECTED.getCode(), handledAppeal.getAppealStatus());
        assertEquals(adminId, handledAppeal.getHandleAdminId());
        assertEquals(restoreRequest.getHandleResult(), handledAppeal.getHandleResult());
        assertNotNull(handledAppeal.getHandleTime());

        ArgumentCaptor<ErrandOrder> orderCaptor = ArgumentCaptor.forClass(ErrandOrder.class);
        verify(errandOrderMapper).updateById(orderCaptor.capture());
        ErrandOrder restoredOrder = orderCaptor.getValue();
        assertEquals(0, restoredOrder.getAppealFlag());
        assertEquals(appealToRestore.getBeforeOrderStatus(), restoredOrder.getOrderStatus());

        AppealRecord appealToSet = buildAppeal(201L, AppealStatusEnum.PROCESSING.getCode());
        when(appealRecordMapper.selectById(appealToSet.getId())).thenReturn(appealToSet);
        AppealHandleRequest setRequest = buildHandleRequest(AppealStatusEnum.UPHELD.getCode(),
                OrderStatusEnum.CLOSED.getCode());
        appealService.handle(appealToSet.getId(), adminId, setRequest);

        verify(errandOrderMapper, org.mockito.Mockito.times(2)).updateById(orderCaptor.capture());
        ErrandOrder setOrder = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertEquals(OrderStatusEnum.CLOSED.getCode(), setOrder.getOrderStatus());
    }

    /**
     * Handling a missing appeal throws APPEAL_NOT_FOUND.
     */
    @Test
    @DisplayName("handle fails when appeal does not exist")
    void shouldRejectMissingAppealHandle() {
        when(appealRecordMapper.selectById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.handle(404L, 9L,
                        buildHandleRequest(AppealStatusEnum.REJECTED.getCode(), null)));

        assertEquals(ErrorCode.APPEAL_NOT_FOUND.getCode(), exception.getCode());
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    /**
     * Appeals outside pending or processing states cannot be handled.
     */
    @Test
    @DisplayName("handle fails when appeal is not pending or processing")
    void shouldRejectNonPendingAppealHandle() {
        AppealRecord appeal = buildAppeal(200L, AppealStatusEnum.REJECTED.getCode());
        when(appealRecordMapper.selectById(appeal.getId())).thenReturn(appeal);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.handle(appeal.getId(), 9L,
                        buildHandleRequest(AppealStatusEnum.CLOSED.getCode(), null)));

        assertEquals(ErrorCode.APPEAL_CANNOT_HANDLE.getCode(), exception.getCode());
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    /**
     * Zero-row conditional appeal update is treated as an appeal handling conflict.
     */
    @Test
    @DisplayName("handle fails when conditional appeal update affects zero rows")
    void shouldRejectWhenHandleConditionalUpdateFails() {
        AppealRecord appeal = buildAppeal(200L, AppealStatusEnum.PENDING.getCode());
        when(appealRecordMapper.selectById(appeal.getId())).thenReturn(appeal);
        when(appealRecordMapper.update(any(AppealRecord.class), any(Wrapper.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.handle(appeal.getId(), 9L,
                        buildHandleRequest(AppealStatusEnum.REJECTED.getCode(), null)));

        assertEquals(ErrorCode.APPEAL_CANNOT_HANDLE.getCode(), exception.getCode());
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    /**
     * Invalid target order status should be rejected before writing appeal records.
     */
    @Test
    @DisplayName("handle fails when result order status is invalid")
    void shouldRejectInvalidResultOrderStatus() {
        AppealHandleRequest request = buildHandleRequest(AppealStatusEnum.REJECTED.getCode(), 99);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.handle(200L, 9L, request));

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
        verify(appealRecordMapper, never()).update(any(AppealRecord.class), any(Wrapper.class));
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    /**
     * Builds a submit request.
     */
    private AppealSubmitRequest buildSubmitRequest(Long orderId) {
        AppealSubmitRequest request = new AppealSubmitRequest();
        request.setOrderId(orderId);
        request.setAppealType(2);
        request.setAppealContent("Runner did not complete the agreed delivery.");
        request.setEvidenceUrls("https://example.com/evidence.png");
        return request;
    }

    /**
     * Builds a handle request.
     */
    private AppealHandleRequest buildHandleRequest(Integer appealStatus, Integer resultOrderStatus) {
        AppealHandleRequest request = new AppealHandleRequest();
        request.setAppealStatus(appealStatus);
        request.setResultOrderStatus(resultOrderStatus);
        request.setResponsibilityType(1);
        request.setRefundDecision(0);
        request.setHandleResult("Handled after evidence review.");
        return request;
    }

    /**
     * Builds a test order.
     */
    private ErrandOrder buildOrder(Long id, Long publisherId, Long runnerId, Integer orderStatus) {
        ErrandOrder order = new ErrandOrder();
        order.setId(id);
        order.setPublisherId(publisherId);
        order.setRunnerId(runnerId);
        order.setOrderStatus(orderStatus);
        order.setAppealFlag(0);
        return order;
    }

    /**
     * Builds a test appeal.
     */
    private AppealRecord buildAppeal(Long id, Integer appealStatus) {
        AppealRecord appeal = new AppealRecord();
        appeal.setId(id);
        appeal.setOrderId(100L);
        appeal.setAppealNo("AP" + id);
        appeal.setApplyUserId(11L);
        appeal.setApplyRole("STUDENT");
        appeal.setAppealType(2);
        appeal.setAppealContent("content");
        appeal.setEvidenceUrls("urls");
        appeal.setAppealStatus(appealStatus);
        appeal.setBeforeOrderStatus(OrderStatusEnum.ACCEPTED.getCode());
        appeal.setCreateTime(LocalDateTime.now());
        return appeal;
    }
}
