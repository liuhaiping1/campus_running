package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.ErrandOrderAddressMapper;
import com.example.backend.mapper.ErrandOrderDetailMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.mapper.RefundRecordMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.RunnerIncomeRecordMapper;
import com.example.backend.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 订单并发安全测试
 * <p>
 * 验证在高并发场景下的数据一致性，包括：
 * <ul>
 *   <li>多线程同时接单，仅一个成功</li>
 *   <li>多线程同时取消订单，仅一个成功</li>
 *   <li>多线程同时执行状态流转，仅一个成功</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单并发安全测试")
class OrderConcurrencyTest {

    @Mock
    private ErrandOrderMapper errandOrderMapper;

    @Mock
    private ErrandCategoryMapper errandCategoryMapper;

    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;

    @Mock
    private RunnerAuthMapper runnerAuthMapper;

    @Mock
    private RefundRecordMapper refundRecordMapper;

    @Mock
    private RunnerIncomeRecordMapper runnerIncomeRecordMapper;

    @Mock
    private ErrandOrderAddressMapper errandOrderAddressMapper;

    @Mock
    private ErrandOrderDetailMapper errandOrderDetailMapper;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Long ORDER_ID = 100L;
    private static final Long PUBLISHER_ID = 1L;

    /**
     * 构建已认证的跑腿员 RunnerAuth
     */
    private RunnerAuth buildRunnerAuth(Long userId) {
        RunnerAuth auth = new RunnerAuth();
        auth.setUserId(userId);
        auth.setAuthStatus(AuthStatusEnum.APPROVED.getCode());
        auth.setCurrentFlag(1);
        return auth;
    }

    /**
     * 构建满足接单条件的订单
     */
    private ErrandOrder buildAcceptableOrder() {
        ErrandOrder order = new ErrandOrder();
        order.setId(ORDER_ID);
        order.setOrderNo("ER20240109001");
        order.setPublisherId(PUBLISHER_ID);
        order.setRunnerId(null);
        order.setOrderStatus(OrderStatusEnum.WAITING_ACCEPT.getCode());
        order.setPayStatus(PayStatusEnum.PAID.getCode());
        return order;
    }

    // =========================================================================
    // 并发抢单测试
    // =========================================================================

    @Nested
    @DisplayName("并发抢单")
    class ConcurrentAcceptTests {

        /**
         * 测试多线程同时接单，仅一个成功
         * <p>
         * 模拟10个跑腿员同时接同一个订单，验证：
         * <ul>
         *   <li>只有1个线程成功接单</li>
         *   <li>其他9个线程抛出 ORDER_ALREADY_ACCEPTED 异常</li>
         *   <li>条件更新只执行一次</li>
         *   <li>状态日志只写入一次</li>
         * </ul>
         */
        @Test
        @DisplayName("10个跑腿员同时接单，仅一个成功")
        void shouldOnlyOneRunnerAcceptSuccessfully() throws Exception {
            // Given: 10个跑腿员
            int threadCount = 10;
            Long[] runnerIds = new Long[threadCount];
            for (int i = 0; i < threadCount; i++) {
                runnerIds[i] = 100L + i;
            }

            // Mock: 所有跑腿员都已认证
            for (Long runnerId : runnerIds) {
                when(runnerAuthMapper.selectOne(any()))
                        .thenReturn(buildRunnerAuth(runnerId));
            }

            // Mock: 订单存在且满足接单条件
            when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(buildAcceptableOrder());

            // Mock: 条件更新 - 第一次返回1（成功），后续返回0（已被接单）
            AtomicInteger updateCallCount = new AtomicInteger(0);
            doAnswer(invocation -> {
                int count = updateCallCount.incrementAndGet();
                return count == 1 ? 1 : 0; // 只有第一次成功
            }).when(errandOrderMapper).update(any(), any());

            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: 多线程同时接单
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            AtomicReference<Exception> firstException = new AtomicReference<>();

            for (int i = 0; i < threadCount; i++) {
                final Long runnerId = runnerIds[i];
                executor.submit(() -> {
                    try {
                        startLatch.await(5, TimeUnit.SECONDS); // 等待同时开始
                        orderService.accept(ORDER_ID, runnerId);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getCode().equals(ErrorCode.ORDER_ALREADY_ACCEPTED.getCode())) {
                            failureCount.incrementAndGet();
                        } else {
                            firstException.compareAndSet(null, e);
                        }
                    } catch (Exception e) {
                        firstException.compareAndSet(null, e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 触发所有线程同时开始
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "所有线程应在10秒内完成");

            executor.shutdown();

            // Then: 验证结果
            assertNull(firstException.get(), "不应有非预期异常: " +
                    (firstException.get() != null ? firstException.get().getMessage() : ""));
            assertEquals(1, successCount.get(), "应有且仅有一个线程成功接单");
            assertEquals(threadCount - 1, failureCount.get(), "其他线程应抛出 ORDER_ALREADY_ACCEPTED");

            // 验证条件更新被调用了 threadCount 次
            verify(errandOrderMapper, times(threadCount)).update(any(), any());
            // 验证状态日志只写入一次
            verify(orderStatusLogMapper, times(1)).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试接单条件更新的SQL包含并发控制条件
         * <p>
         * 验证条件更新的WHERE子句包含 runner_id IS NULL、order_status、pay_status，
         * 确保并发时只有满足条件的更新才能成功。
         */
        @Test
        @DisplayName("接单条件更新包含并发控制条件")
        void shouldAcceptUseConditionalUpdate() {
            // Given
            Long runnerId = 2L;
            when(runnerAuthMapper.selectOne(any())).thenReturn(buildRunnerAuth(runnerId));
            when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(buildAcceptableOrder());
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When
            orderService.accept(ORDER_ID, runnerId);

            // Then: 验证条件更新的SQL
            @SuppressWarnings("unchecked")
            var wrapperCaptor = org.mockito.ArgumentCaptor.forClass(UpdateWrapper.class);
            verify(errandOrderMapper).update(any(), wrapperCaptor.capture());

            String sql = wrapperCaptor.getValue().getSqlSegment();
            assertTrue(sql.contains("runner_id"), "WHERE 应包含 runner_id");
            assertTrue(sql.contains("order_status"), "WHERE 应包含 order_status");
            assertTrue(sql.contains("pay_status"), "WHERE 应包含 pay_status");
        }

        /**
         * 测试并发接单时，条件更新返回0应抛出异常
         * <p>
         * 当条件更新返回0时，说明订单已被其他人接单，
         * 应抛出 ORDER_ALREADY_ACCEPTED 异常。
         */
        @Test
        @DisplayName("条件更新返回0时应抛出 ORDER_ALREADY_ACCEPTED")
        void shouldThrowWhenConditionalUpdateReturnsZero() {
            // Given
            Long runnerId = 2L;
            when(runnerAuthMapper.selectOne(any())).thenReturn(buildRunnerAuth(runnerId));
            when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(buildAcceptableOrder());
            doReturn(0).when(errandOrderMapper).update(any(), any()); // 条件更新失败

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.accept(ORDER_ID, runnerId));
            assertEquals(ErrorCode.ORDER_ALREADY_ACCEPTED.getCode(), exception.getCode());

            // 验证状态日志未写入
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }
    }

    // =========================================================================
    // 并发状态流转测试
    // =========================================================================

    @Nested
    @DisplayName("并发状态流转")
    class ConcurrentStatusFlowTests {

        /**
         * 测试多线程同时执行contact，仅一个成功
         * <p>
         * 模拟多个线程同时对同一订单执行contact操作，
         * 验证条件更新保证只有一个线程成功。
         */
        @Test
        @DisplayName("多线程同时contact，仅一个成功")
        void shouldOnlyOneContactSucceed() throws Exception {
            // Given
            int threadCount = 5;
            Long runnerId = 2L;

            ErrandOrder order = new ErrandOrder();
            order.setId(ORDER_ID);
            order.setOrderNo("ER20240109002");
            order.setPublisherId(PUBLISHER_ID);
            order.setRunnerId(runnerId);
            order.setOrderStatus(OrderStatusEnum.ACCEPTED.getCode());

            when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);

            AtomicInteger updateCallCount = new AtomicInteger(0);
            doAnswer(invocation -> {
                int count = updateCallCount.incrementAndGet();
                return count == 1 ? 1 : 0;
            }).when(errandOrderMapper).update(any(), any());

            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        orderService.contact(ORDER_ID, runnerId);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getCode().equals(ErrorCode.ORDER_STATUS_CONFLICT.getCode())) {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Then
            assertEquals(1, successCount.get(), "应有且仅有一个线程成功");
            assertEquals(threadCount - 1, failureCount.get(), "其他线程应抛出 ORDER_STATUS_CONFLICT");
        }

        /**
         * 测试多线程同时执行complete，仅一个成功
         * <p>
         * complete 方法会创建收益记录，验证并发时不会重复创建。
         */
        @Test
        @DisplayName("多线程同时complete，仅一个成功且收益记录不重复")
        void shouldOnlyOneCompleteSucceed() throws Exception {
            // Given
            int threadCount = 5;
            Long publisherId = PUBLISHER_ID;
            Long runnerId = 2L;

            ErrandOrder order = new ErrandOrder();
            order.setId(ORDER_ID);
            order.setOrderNo("ER20240109003");
            order.setPublisherId(publisherId);
            order.setRunnerId(runnerId);
            order.setOrderStatus(OrderStatusEnum.DELIVERED.getCode());
            order.setEstimatedRunnerIncome(new java.math.BigDecimal("10.00"));

            when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);

            AtomicInteger updateCallCount = new AtomicInteger(0);
            doAnswer(invocation -> {
                int count = updateCallCount.incrementAndGet();
                return count == 1 ? 1 : 0;
            }).when(errandOrderMapper).update(any(), any());

            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(runnerIncomeRecordMapper.insert(any(com.example.backend.entity.RunnerIncomeRecord.class))).thenReturn(1);

            // Mock: 收益记录查询返回0（不存在）
            when(runnerIncomeRecordMapper.selectCount(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        orderService.complete(ORDER_ID, publisherId);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getCode().equals(ErrorCode.ORDER_STATUS_CONFLICT.getCode())) {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Then
            assertEquals(1, successCount.get(), "应有且仅有一个线程成功");
            assertEquals(threadCount - 1, failureCount.get(), "其他线程应抛出 ORDER_STATUS_CONFLICT");
            // 收益记录只写入一次
            verify(runnerIncomeRecordMapper, times(1)).insert(any(com.example.backend.entity.RunnerIncomeRecord.class));
        }
    }

    // =========================================================================
    // 并发取消订单测试
    // =========================================================================

    @Nested
    @DisplayName("并发取消订单")
    class ConcurrentCancelTests {

        /**
         * 测试多线程同时取消订单，仅一个成功
         * <p>
         * 模拟发布人和跑腿员同时取消同一订单，
         * 验证条件更新保证只有一个线程成功。
         */
        @Test
        @DisplayName("多线程同时取消，仅一个成功")
        void shouldOnlyOneCancelSucceed() throws Exception {
            // Given
            int threadCount = 5;
            Long publisherId = PUBLISHER_ID;
            Long runnerId = 2L;

            ErrandOrder order = new ErrandOrder();
            order.setId(ORDER_ID);
            order.setOrderNo("ER20240109004");
            order.setPublisherId(publisherId);
            order.setRunnerId(runnerId);
            order.setOrderStatus(OrderStatusEnum.ACCEPTED.getCode());
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            order.setOrderAmount(new java.math.BigDecimal("10.00"));

            when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);

            AtomicInteger updateCallCount = new AtomicInteger(0);
            doAnswer(invocation -> {
                int count = updateCallCount.incrementAndGet();
                return count == 1 ? 1 : 0;
            }).when(errandOrderMapper).update(any(), any());

            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(refundRecordMapper.insert(any(com.example.backend.entity.RefundRecord.class))).thenReturn(1);

            // When: 发布人和跑腿员同时取消
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final Long userId = (i % 2 == 0) ? publisherId : runnerId;
                executor.submit(() -> {
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        var request = new com.example.backend.dto.request.OrderCancelRequest();
                        request.setCancelReason("并发测试取消");
                        orderService.cancel(ORDER_ID, userId, request);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getCode().equals(ErrorCode.ORDER_STATUS_CONFLICT.getCode())) {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Then
            assertEquals(1, successCount.get(), "应有且仅有一个线程成功取消");
            assertEquals(threadCount - 1, failureCount.get(), "其他线程应抛出 ORDER_STATUS_CONFLICT");
            // 状态日志只写入一次
            verify(orderStatusLogMapper, times(1)).insert(any(OrderStatusLog.class));
            // 退款记录只写入一次（已支付订单）
            verify(refundRecordMapper, times(1)).insert(any(com.example.backend.entity.RefundRecord.class));
        }
    }
}
