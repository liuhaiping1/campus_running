package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.SettlementStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.EvaluationSubmitRequest;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.OrderEvaluation;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.entity.RunnerIncomeRecord;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.RunnerIncomeRecordMapper;
import com.example.backend.service.impl.EvaluationServiceImpl;
import com.example.backend.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EvaluationService 单元测试类
 * <p>
 * 使用 Mockito 对 {@link EvaluationServiceImpl} 进行单元测试，
 * 覆盖评价提交的七种核心场景及 {@link OrderServiceImpl#complete()} 收益记录生成的两种场景。
 * <p>
 * 测试采用 @Nested 分组，将评价提交和收益记录生成相关测试分别组织，
 * 每组包含正常场景和各类异常场景，确保业务逻辑的完整性验证。
 * <p>
 * Mock 策略：使用 {@code @Mock} 模拟所有 Mapper 层依赖，
 * 通过 {@code @InjectMocks} 将模拟对象注入被测 Service 实例，
 * 完全隔离数据库和 Spring 容器依赖。
 *
 * @author campus_running
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationService 单元测试")
class EvaluationServiceTest {

    // =========================================================================
    // Mock 依赖（评价服务）
    // =========================================================================

    /** Mock 订单评价 Mapper */
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;

    /** Mock 跑腿订单 Mapper */
    @Mock
    private ErrandOrderMapper errandOrderMapper;

    /** Mock 订单状态流转日志 Mapper */
    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;

    /** 自动注入 Mock 依赖的被测对象 */
    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    // =========================================================================
    // ArgumentCaptor
    // =========================================================================

    /** OrderEvaluation 捕获器 */
    @Captor
    private ArgumentCaptor<OrderEvaluation> evaluationCaptor;

    /** OrderStatusLog 捕获器 */
    @Captor
    private ArgumentCaptor<OrderStatusLog> logCaptor;

    // =========================================================================
    // 通用测试数据
    // =========================================================================

    /** 测试用用户ID（发布人） */
    private static final Long USER_ID = 1L;

    /** 测试用评价提交请求 */
    private EvaluationSubmitRequest evaluationRequest;

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 构建测试用订单对象
     *
     * @param id          订单ID
     * @param orderNo     订单编号
     * @param publisherId 发布人ID
     * @param runnerId    接单人ID
     * @param categoryId  分类ID
     * @param orderStatus 订单状态
     * @return 订单实体
     */
    private ErrandOrder buildOrder(Long id, String orderNo, Long publisherId, Long runnerId,
                                    Long categoryId, Integer orderStatus) {
        ErrandOrder order = new ErrandOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setPublisherId(publisherId);
        order.setRunnerId(runnerId);
        order.setCategoryId(categoryId);
        order.setTitle("测试任务");
        order.setOrderDesc("测试描述");
        order.setPickupAddress("测试取件点");
        order.setDeliveryAddress("测试送达点");
        order.setDistanceKm(BigDecimal.valueOf(1.0));
        order.setBaseFee(BigDecimal.valueOf(3.00));
        order.setDistanceFee(BigDecimal.ZERO);
        order.setWeightFee(BigDecimal.ZERO);
        order.setTimeFee(BigDecimal.ZERO);
        order.setTipFee(BigDecimal.ZERO);
        order.setOrderAmount(BigDecimal.valueOf(3.00));
        order.setPlatformCommission(BigDecimal.ZERO);
        order.setEstimatedRunnerIncome(BigDecimal.valueOf(5.00));
        order.setOrderStatus(orderStatus);
        order.setPayStatus(1);
        order.setSettlementStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    // =========================================================================
    // 评价提交测试组
    // =========================================================================

    /**
     * 评价提交功能测试组
     * <p>
     * 覆盖评价提交的七种核心场景：
     * <ol>
     *   <li>评价成功 —— 发布人对已完成的订单进行评价</li>
     *   <li>非发布人评价失败 —— 抛出 ORDER_NOT_OWNED 异常</li>
     *   <li>未完成订单评价失败 —— 抛出 ORDER_CANNOT_EVALUATE 异常</li>
     *   <li>订单无跑腿员评价失败 —— 抛出 ORDER_CANNOT_EVALUATE 异常</li>
     *   <li>重复评价失败 —— 抛出 EVALUATION_ALREADY_EXISTS 异常</li>
     *   <li>订单不存在评价失败 —— 抛出 ORDER_NOT_FOUND 异常</li>
     * </ol>
     */
    @Nested
    @DisplayName("评价提交")
    class SubmitEvaluationTests {

        @BeforeEach
        void setUp() {
            evaluationRequest = new EvaluationSubmitRequest();
            evaluationRequest.setOrderId(100L);
            evaluationRequest.setScore(5);
            evaluationRequest.setContent("服务很好，速度快！");
        }

        /**
         * 测试评价成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单查询返回已完成的订单，发布人为当前用户，有跑腿员接单</li>
         *   <li>订单评价查询返回空（未评价过）</li>
         *   <li>评价插入成功，返回评价ID</li>
         *   <li>状态流转日志记录 beforeStatus=COMPLETED, afterStatus=COMPLETED, triggerAction=EVALUATE_ORDER, operatorRole=STUDENT</li>
         * </ul>
         */
        @Test
        @DisplayName("评价成功")
        void shouldSubmitEvaluationSuccessfully() {
            // Given: 订单已完成，发布人为当前用户，有跑腿员接单
            Long orderId = 100L;
            Long runnerId = 2L;
            ErrandOrder order = buildOrder(orderId, "ER20240110001", USER_ID, runnerId, 1L,
                    OrderStatusEnum.COMPLETED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // 未评价过
            when(orderEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // insert 成功后设置评价ID
            doAnswer(inv -> {
                OrderEvaluation eval = inv.getArgument(0);
                eval.setId(200L);
                return 1;
            }).when(orderEvaluationMapper).insert(any(OrderEvaluation.class));

            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: 执行评价提交
            Long evaluationId = evaluationService.submit(USER_ID, evaluationRequest);

            // Then: 返回评价ID
            assertEquals(Long.valueOf(200L), evaluationId, "应返回新创建的评价ID");

            // 验证评价记录插入
            verify(orderEvaluationMapper).insert(evaluationCaptor.capture());
            OrderEvaluation savedEval = evaluationCaptor.getValue();
            assertEquals(orderId, savedEval.getOrderId(), "订单ID应一致");
            assertEquals(USER_ID, savedEval.getPublisherId(), "发布人ID应一致");
            assertEquals(runnerId, savedEval.getRunnerId(), "跑腿员ID应一致");
            assertEquals(Integer.valueOf(5), savedEval.getStarScore(), "评分应为5");
            assertEquals("服务很好，速度快！", savedEval.getContent(), "评价内容应一致");

            // 验证状态流转日志
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            OrderStatusLog statusLog = logCaptor.getValue();
            assertEquals(orderId, statusLog.getOrderId(), "日志关联的订单ID应一致");
            assertEquals("ER20240110001", statusLog.getOrderNo(), "日志订单号应一致");
            assertEquals(OrderStatusEnum.COMPLETED.getCode(), statusLog.getBeforeStatus(),
                    "变更前状态应为 COMPLETED(7)");
            assertEquals(OrderStatusEnum.COMPLETED.getCode(), statusLog.getAfterStatus(),
                    "变更后状态应为 COMPLETED(7)");
            assertEquals("EVALUATE_ORDER", statusLog.getTriggerAction(), "触发动作应为 EVALUATE_ORDER");
            assertEquals(USER_ID, statusLog.getOperatorUserId(), "操作人ID应为发布人ID");
            assertEquals("STUDENT", statusLog.getOperatorRole(), "操作人角色应为 STUDENT");
        }

        /**
         * 测试非发布人评价失败
         * <p>
         * 当用户不是订单发布人时，应抛出 {@link BusinessException}，
         * 错误码为 {@link ErrorCode#ORDER_NOT_OWNED}(5002)。
         */
        @Test
        @DisplayName("非发布人评价失败")
        void shouldThrowWhenNotPublisher() {
            // Given: 订单存在但发布人是其他用户
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER20240110002", 99L, 2L, 1L,
                    OrderStatusEnum.COMPLETED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evaluationService.submit(USER_ID, evaluationRequest),
                    "非发布人评价时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_OWNED.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_OWNED(5002)");
            assertEquals(ErrorCode.ORDER_NOT_OWNED.getMessage(), exception.getMessage(),
                    "错误信息应为: 无权操作该订单");

            // 验证未执行后续操作
            verify(orderEvaluationMapper, never()).insert(any(OrderEvaluation.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试未完成订单评价失败
         * <p>
         * 当订单状态不是 COMPLETED 时，应抛出 {@link BusinessException}，
         * 错误码为 {@link ErrorCode#ORDER_CANNOT_EVALUATE}(5010)。
         */
        @Test
        @DisplayName("未完成订单评价失败")
        void shouldThrowWhenOrderNotCompleted() {
            // Given: 订单状态为 DELIVERED(6)，不是 COMPLETED(7)
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER20240110003", USER_ID, 2L, 1L,
                    OrderStatusEnum.DELIVERED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evaluationService.submit(USER_ID, evaluationRequest),
                    "未完成订单评价时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_CANNOT_EVALUATE.getCode(), exception.getCode(),
                    "错误码应为 ORDER_CANNOT_EVALUATE(5010)");
            assertEquals(ErrorCode.ORDER_CANNOT_EVALUATE.getMessage(), exception.getMessage(),
                    "错误信息应为: 该订单不允许评价");

            // 验证未执行后续操作
            verify(orderEvaluationMapper, never()).insert(any(OrderEvaluation.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试订单无跑腿员评价失败
         * <p>
         * 当订单 runnerId 为 null 时，应抛出 {@link BusinessException}，
         * 错误码为 {@link ErrorCode#ORDER_CANNOT_EVALUATE}(5010)。
         */
        @Test
        @DisplayName("订单无跑腿员评价失败")
        void shouldThrowWhenNoRunner() {
            // Given: 订单已完成但 runnerId 为 null
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER20240110004", USER_ID, null, 1L,
                    OrderStatusEnum.COMPLETED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evaluationService.submit(USER_ID, evaluationRequest),
                    "订单无跑腿员时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_CANNOT_EVALUATE.getCode(), exception.getCode(),
                    "错误码应为 ORDER_CANNOT_EVALUATE(5010)");
            assertEquals(ErrorCode.ORDER_CANNOT_EVALUATE.getMessage(), exception.getMessage(),
                    "错误信息应为: 该订单不允许评价");

            // 验证未执行后续操作
            verify(orderEvaluationMapper, never()).insert(any(OrderEvaluation.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试重复评价失败
         * <p>
         * 当订单已经评价过（selectCount > 0）时，应抛出 {@link BusinessException}，
         * 错误码为 {@link ErrorCode#EVALUATION_ALREADY_EXISTS}(6001)。
         */
        @Test
        @DisplayName("重复评价失败")
        void shouldThrowWhenEvaluationAlreadyExists() {
            // Given: 订单已完成，有跑腿员接单，但已评价过
            Long orderId = 100L;
            Long runnerId = 2L;
            ErrandOrder order = buildOrder(orderId, "ER20240110005", USER_ID, runnerId, 1L,
                    OrderStatusEnum.COMPLETED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // 已存在评价
            when(orderEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evaluationService.submit(USER_ID, evaluationRequest),
                    "重复评价时应抛出 BusinessException");

            assertEquals(ErrorCode.EVALUATION_ALREADY_EXISTS.getCode(), exception.getCode(),
                    "错误码应为 EVALUATION_ALREADY_EXISTS(6001)");
            assertEquals(ErrorCode.EVALUATION_ALREADY_EXISTS.getMessage(), exception.getMessage(),
                    "错误信息应为: 该订单已评价");

            // 验证未执行后续操作
            verify(orderEvaluationMapper, never()).insert(any(OrderEvaluation.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试并发重复评价失败
         * <p>
         * 当查询未发现评价但插入时触发唯一键冲突，应转换为 {@link BusinessException}，
         * 错误码为 {@link ErrorCode#EVALUATION_ALREADY_EXISTS}(6001)。
         */
        @Test
        @DisplayName("并发重复评价失败")
        void shouldThrowBusinessExceptionWhenDuplicateKeyOnInsert() {
            // Given: 订单已完成，插入评价时发生唯一键冲突
            Long orderId = 100L;
            Long runnerId = 2L;
            ErrandOrder order = buildOrder(orderId, "ER20240110006", USER_ID, runnerId, 1L,
                    OrderStatusEnum.COMPLETED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(orderEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            doThrow(new DuplicateKeyException("Duplicate entry"))
                    .when(orderEvaluationMapper).insert(any(OrderEvaluation.class));

            // When & Then: 应抛出业务异常
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evaluationService.submit(USER_ID, evaluationRequest),
                    "并发重复评价时应抛出 BusinessException");

            assertEquals(ErrorCode.EVALUATION_ALREADY_EXISTS.getCode(), exception.getCode(),
                    "错误码应为 EVALUATION_ALREADY_EXISTS(6001)");
            assertEquals(ErrorCode.EVALUATION_ALREADY_EXISTS.getMessage(), exception.getMessage(),
                    "错误信息应为: 该订单已评价");

            // 验证评价插入失败后不写状态日志
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试订单不存在评价失败
         * <p>
         * 当 errandOrderMapper.selectById 返回 null 时，应抛出 {@link BusinessException}，
         * 错误码为 {@link ErrorCode#ORDER_NOT_FOUND}(5001)。
         */
        @Test
        @DisplayName("订单不存在评价失败")
        void shouldThrowWhenOrderNotFound() {
            // Given: 订单不存在
            Long orderId = 999L;
            evaluationRequest.setOrderId(orderId);
            when(errandOrderMapper.selectById(orderId)).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evaluationService.submit(USER_ID, evaluationRequest),
                    "订单不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_FOUND(5001)");
            assertEquals(ErrorCode.ORDER_NOT_FOUND.getMessage(), exception.getMessage(),
                    "错误信息应为: 订单不存在");

            // 验证未执行后续操作
            verify(orderEvaluationMapper, never()).insert(any(OrderEvaluation.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }
    }

    // =========================================================================
    // 收益记录生成测试组（针对 OrderServiceImpl.complete）
    // =========================================================================

    /**
     * 收益记录生成测试组
     * <p>
     * 覆盖 complete() 方法中收益记录生成的两种核心场景：
     * <ol>
     *   <li>完成订单成功生成收益记录 —— 首次完成时创建收益记录</li>
     *   <li>收益记录已存在时完成成功但不重复生成 —— 避免重复创建收益记录</li>
     * </ol>
     * <p>
     * 该测试组使用独立的 Mock 配置测试 {@link OrderServiceImpl#complete()} 方法
     * 的收益记录生成逻辑，与评价提交测试隔离。
     */
    @Nested
    @DisplayName("收益记录生成")
    class CompleteIncomeRecordTests {

        /** Mock 跑腿订单 Mapper */
        @Mock
        private ErrandOrderMapper completeErrandOrderMapper;

        /** Mock 订单状态流转日志 Mapper */
        @Mock
        private OrderStatusLogMapper completeOrderStatusLogMapper;

        /** Mock 跑腿收益记录 Mapper */
        @Mock
        private RunnerIncomeRecordMapper completeRunnerIncomeRecordMapper;

        /** 自动注入 Mock 依赖的被测对象 */
        @InjectMocks
        private OrderServiceImpl orderService;

        /** OrderStatusLog 捕获器 */
        @Captor
        private ArgumentCaptor<OrderStatusLog> completeLogCaptor;

        /** RunnerIncomeRecord 捕获器 */
        @Captor
        private ArgumentCaptor<RunnerIncomeRecord> incomeRecordCaptor;

        /**
         * 测试 complete 成功生成收益记录
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单状态为 DELIVERED(6)，发布人为当前用户，有跑腿员接单</li>
         *   <li>收益记录查询返回 0（不存在）</li>
         *   <li>订单状态更新成功</li>
         *   <li>状态流转日志记录 beforeStatus=DELIVERED, afterStatus=COMPLETED, triggerAction=COMPLETE_ORDER, operatorRole=STUDENT</li>
         *   <li>创建收益记录，金额为 estimatedRunnerIncome=5.00，结算状态为 PENDING(0)</li>
         * </ul>
         */
        @Test
        @DisplayName("complete 成功生成收益记录")
        void shouldGenerateIncomeRecordOnComplete() {
            // Given
            Long orderId = 100L;
            Long publisherId = 1L;
            Long runnerId = 2L;
            BigDecimal estimatedIncome = BigDecimal.valueOf(5.00);

            ErrandOrder order = buildOrder(orderId, "ER20240111001", publisherId, runnerId, 1L,
                    OrderStatusEnum.DELIVERED.getCode());
            order.setEstimatedRunnerIncome(estimatedIncome);

            when(completeErrandOrderMapper.selectById(orderId)).thenReturn(order);
            when(completeRunnerIncomeRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            doReturn(1).when(completeErrandOrderMapper).update(any(), any());
            when(completeOrderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: 发布人确认完成
            orderService.complete(orderId, publisherId);

            // Then: 验证收益记录创建
            verify(completeRunnerIncomeRecordMapper).insert(incomeRecordCaptor.capture());
            RunnerIncomeRecord incomeRecord = incomeRecordCaptor.getValue();
            assertEquals(orderId, incomeRecord.getOrderId(), "订单ID应一致");
            assertEquals(runnerId, incomeRecord.getRunnerId(), "跑腿员ID应一致");
            assertEquals(estimatedIncome.stripTrailingZeros(),
                    incomeRecord.getIncomeAmount().stripTrailingZeros(),
                    "收益金额应为 5.00");
            assertEquals(Integer.valueOf(SettlementStatusEnum.PENDING.getCode()),
                    incomeRecord.getSettlementStatus(), "结算状态应为 PENDING(0)");

            // 验证状态流转日志
            verify(completeOrderStatusLogMapper).insert(completeLogCaptor.capture());
            OrderStatusLog statusLog = completeLogCaptor.getValue();
            assertEquals(orderId, statusLog.getOrderId(), "日志关联的订单ID应一致");
            assertEquals(OrderStatusEnum.DELIVERED.getCode(), statusLog.getBeforeStatus(),
                    "变更前状态应为 DELIVERED(6)");
            assertEquals(OrderStatusEnum.COMPLETED.getCode(), statusLog.getAfterStatus(),
                    "变更后状态应为 COMPLETED(7)");
            assertEquals("COMPLETE_ORDER", statusLog.getTriggerAction(),
                    "触发动作应为 COMPLETE_ORDER");
            assertEquals(publisherId, statusLog.getOperatorUserId(), "操作人ID应为发布人ID");
            assertEquals("STUDENT", statusLog.getOperatorRole(), "操作人角色应为 STUDENT");
        }

        /**
         * 测试收益记录已存在时 complete 成功但不重复生成
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单状态为 DELIVERED(6)，发布人为当前用户，有跑腿员接单</li>
         *   <li>收益记录查询返回 1（已存在）</li>
         *   <li>订单状态更新成功</li>
         *   <li>状态流转日志正常记录</li>
         *   <li>不重复创建收益记录 —— runnerIncomeRecordMapper.insert 从未被调用</li>
         * </ul>
         */
        @Test
        @DisplayName("收益记录已存在时 complete 成功但不重复生成")
        void shouldNotGenerateDuplicateIncomeRecordOnComplete() {
            // Given
            Long orderId = 100L;
            Long publisherId = 1L;
            Long runnerId = 2L;
            BigDecimal estimatedIncome = BigDecimal.valueOf(5.00);

            ErrandOrder order = buildOrder(orderId, "ER20240111002", publisherId, runnerId, 1L,
                    OrderStatusEnum.DELIVERED.getCode());
            order.setEstimatedRunnerIncome(estimatedIncome);

            when(completeErrandOrderMapper.selectById(orderId)).thenReturn(order);
            when(completeRunnerIncomeRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            doReturn(1).when(completeErrandOrderMapper).update(any(), any());
            when(completeOrderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: 发布人确认完成
            orderService.complete(orderId, publisherId);

            // Then: 验证状态更新成功
            verify(completeErrandOrderMapper).update(any(), any());

            // 验证状态流转日志正常记录
            verify(completeOrderStatusLogMapper).insert(any(OrderStatusLog.class));

            // 验证不重复创建收益记录
            verify(completeRunnerIncomeRecordMapper, never()).insert(any(RunnerIncomeRecord.class));
        }
    }
}
