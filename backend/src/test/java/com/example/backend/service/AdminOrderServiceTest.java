package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.ErrandOrderAddress;
import com.example.backend.entity.ErrandOrderDetail;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.ErrandOrderAddressMapper;
import com.example.backend.mapper.ErrandOrderDetailMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.service.impl.AdminOrderServiceImpl;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminOrderService 单元测试类
 * <p>
 * 使用 Mockito 对 {@link AdminOrderServiceImpl} 进行单元测试，
 * 覆盖管理端订单分页查询和订单详情查看两大功能。
 * </p>
 * <p>
 * Mock 策略：使用 {@code @Mock} 模拟所有 Mapper 层依赖，
 * 通过 {@code @InjectMocks} 将模拟对象注入被测 Service 实例。
 * </p>
 * <p>
 * 与用户端 OrderServiceTest 的关键区别：管理员查询不受发布人/接单人权限限制，
 * detail 方法不校验所有权，list 方法支持更多筛选条件。
 * </p>
 *
 * @author campus_running
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderService 单元测试")
class AdminOrderServiceTest {

    /** Mock 订单 Mapper */
    @Mock
    private ErrandOrderMapper errandOrderMapper;

    /** Mock 分类 Mapper */
    @Mock
    private ErrandCategoryMapper errandCategoryMapper;

    /** Mock 状态日志 Mapper */
    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;

    /** Mock 订单地址快照 Mapper */
    @Mock
    private ErrandOrderAddressMapper errandOrderAddressMapper;

    /** Mock 订单分类扩展详情 Mapper */
    @Mock
    private ErrandOrderDetailMapper errandOrderDetailMapper;

    /** Mock 订单评价 Mapper */
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;

    /** 自动注入 Mock 的被测对象 */
    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    /** 测试用分类 */
    private ErrandCategory testCategory;

    /**
     * 每个测试方法执行前的初始化操作
     * <p>
     * 构建通用的分类对象，供各测试复用。
     * </p>
     */
    @BeforeEach
    void setUp() {
        testCategory = buildCategory(1L, "快递代取");
    }

    // =========================================================================
    // 订单列表分页测试组
    // =========================================================================

    /**
     * 订单列表分页查询测试组
     * <p>
     * 覆盖列表查询的关键场景：
     * <ol>
     *   <li>无筛选条件时返回全部订单</li>
     *   <li>按订单状态筛选</li>
     *   <li>按关键词（订单号/标题）模糊搜索</li>
     *   <li>按时间范围筛选</li>
     *   <li>空结果返回空页</li>
     * </ol>
     */
    @Nested
    @DisplayName("订单列表分页")
    class ListTests {

        /**
         * 测试无筛选条件时返回分页结果
         * <p>
         * 验证点：
         * <ul>
         *   <li>不传入任何筛选参数时查询全部未删除订单</li>
         *   <li>分页信息正确返回</li>
         *   <li>分类名称被正确填充</li>
         *   <li>结果按创建时间倒序排列</li>
         * </ul>
         */
        @Test
        @DisplayName("无筛选条件时应返回全量订单分页")
        void shouldReturnAllOrdersWithoutFilter() {
            // Given: 有2个订单
            ErrandOrder order1 = buildOrder(1L, "ER001", 1L, null, 1L, 1);
            ErrandOrder order2 = buildOrder(2L, "ER002", 3L, 5L, 1L, 2);
            List<ErrandOrder> records = Arrays.asList(order1, order2);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(records);
                        page.setTotal(records.size());
                        return page;
                    });
            when(errandCategoryMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testCategory));

            // When: 管理员查询订单列表（无筛选条件）
            IPage<OrderVO> result = adminOrderService.list(
                    null, null, null, null, null, null, 1, 10);

            // Then: 验证分页结果
            assertNotNull(result, "返回结果不应为null");
            assertEquals(2, result.getTotal(), "总数应为2");
            assertEquals(2, result.getRecords().size(), "记录数应为2");

            // 验证分类名称被填充
            OrderVO vo1 = result.getRecords().get(0);
            assertEquals(Long.valueOf(1L), vo1.getId(), "订单ID应一致");
            assertEquals("ER001", vo1.getOrderNo(), "订单编号应一致");
            assertEquals("快递代取", vo1.getCategoryName(), "分类名称应正确");

            // 管理员查询不需要 userId，验证 selectPage 和 batchIds 被调用
            verify(errandOrderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
            verify(errandCategoryMapper).selectBatchIds(anyList());
        }

        /**
         * 测试按订单状态筛选
         * <p>
         * 验证传入 orderStatus 参数后，仅返回状态匹配的订单。
         * </p>
         */
        @Test
        @DisplayName("应按订单状态筛选返回匹配的订单")
        void shouldFilterByOrderStatus() {
            // Given: 一个待支付订单
            ErrandOrder unpaidOrder = buildOrder(1L, "ER003", 1L, null, 1L, 0);
            List<ErrandOrder> filtered = Collections.singletonList(unpaidOrder);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(filtered);
                        page.setTotal(1);
                        return page;
                    });
            when(errandCategoryMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testCategory));

            // When: 筛选订单状态=0（待支付）
            IPage<OrderVO> result = adminOrderService.list(
                    0, null, null, null, null, null, 1, 10);

            // Then: 仅返回状态匹配的订单
            assertEquals(1, result.getTotal(), "筛选后总数应为1");
            assertEquals(Integer.valueOf(0), result.getRecords().get(0).getOrderStatus(),
                    "返回订单的订单状态应为0");
        }

        /**
         * 测试按关键词模糊搜索订单号或标题
         * <p>
         * 验证 keyword 参数能同时对 orderNo 和 title 进行模糊匹配。
         * 传入 "快递" 应能匹配标题含"快递"的订单。
         * </p>
         */
        @Test
        @DisplayName("应按关键词模糊匹配订单号或标题")
        void shouldFilterByKeyword() {
            // Given: 匹配关键词的订单
            ErrandOrder matchedOrder = buildOrder(1L, "ER100", 1L, null, 1L, 1);
            matchedOrder.setTitle("代取快递");
            List<ErrandOrder> filtered = Collections.singletonList(matchedOrder);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(filtered);
                        page.setTotal(1);
                        return page;
                    });
            when(errandCategoryMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testCategory));

            // When: 关键词搜索 "快递"
            IPage<OrderVO> result = adminOrderService.list(
                    null, null, null, "快递", null, null, 1, 10);

            // Then: 返回匹配关键词的订单
            assertEquals(1, result.getTotal(), "关键词搜索后总数应为1");
            assertEquals("代取快递", result.getRecords().get(0).getTitle(),
                    "返回订单的标题应包含关键词");
        }

        /**
         * 测试按时间范围筛选
         * <p>
         * 验证 startTime 和 endTime 参数对 createTime 的区间筛选生效。
         * </p>
         */
        @Test
        @DisplayName("应按创建时间范围筛选")
        void shouldFilterByTimeRange() {
            // Given: 在时间范围内的订单
            ErrandOrder order = buildOrder(1L, "ER004", 1L, null, 1L, 1);
            List<ErrandOrder> filtered = Collections.singletonList(order);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(filtered);
                        page.setTotal(1);
                        return page;
                    });
            when(errandCategoryMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testCategory));

            // When: 指定时间范围
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            IPage<OrderVO> result = adminOrderService.list(
                    null, null, null, null, start, end, 1, 10);

            // Then: 返回时间范围内的订单
            assertEquals(1, result.getTotal(), "时间筛选后总数应为1");
        }

        /**
         * 测试空结果时返回空页
         * <p>
         * 当查询条件无匹配时，返回空分页结果。
         * </p>
         */
        @Test
        @DisplayName("无匹配订单时应返回空页")
        void shouldReturnEmptyPageForNoMatches() {
            // Given: 无匹配订单
            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(new ArrayList<>());
                        page.setTotal(0L);
                        return page;
                    });

            // When: 查询不存在的状态
            IPage<OrderVO> result = adminOrderService.list(
                    999, null, null, null, null, null, 1, 10);

            // Then: 返回空页
            assertNotNull(result, "返回结果不应为null");
            assertEquals(0L, result.getTotal(), "总数应为0");
            assertTrue(result.getRecords().isEmpty(), "记录列表应为空");

            // 分类查询不应被调用（空列表时 skip 批量查询）
            verify(errandCategoryMapper, never()).selectBatchIds(anyList());
        }
    }

    // =========================================================================
    // 订单详情测试组
    // =========================================================================

    /**
     * 订单详情查询测试组
     * <p>
     * 覆盖管理员查看订单详情的四种核心场景：
     * <ol>
     *   <li>管理员查看任意订单详情成功 —— 不受所属人限制</li>
     *   <li>管理员查看非本人订单详情成功 —— 所有权绕过验证</li>
     *   <li>订单不存在时抛出异常</li>
     *   <li>详情包含状态流转日志</li>
     * </ol>
     */
    @Nested
    @DisplayName("订单详情")
    class DetailTests {

        /**
         * 测试管理员查看任意订单详情成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>管理员不需要是订单的发布人或接单人</li>
         *   <li>分类名称正确填充</li>
         *   <li>状态流转日志被返回</li>
         * </ul>
         */
        @Test
        @DisplayName("管理员应能查看任意订单详情")
        void shouldAllowAdminToViewAnyOrder() {
            // Given: 订单存在，发布人=1L，接单人=2L（都不是管理员5L）
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER20240101001", 1L, 2L, 1L, 2);
            order.setTitle("代购食堂午餐");
            order.setOrderDesc("买一份午餐");
            order.setPickupAddress("食堂一楼");
            order.setDeliveryAddress("3号宿舍楼");
            order.setPickupLng(BigDecimal.valueOf(120.5));
            order.setPickupLat(BigDecimal.valueOf(30.2));
            order.setDeliveryLng(BigDecimal.valueOf(120.8));
            order.setDeliveryLat(BigDecimal.valueOf(30.5));
            order.setDistanceKm(BigDecimal.valueOf(1.0));
            order.setBaseFee(BigDecimal.valueOf(5.00));
            order.setDistanceFee(BigDecimal.ZERO);
            order.setWeightFee(BigDecimal.ZERO);
            order.setTimeFee(BigDecimal.ZERO);
            order.setTipFee(BigDecimal.valueOf(2.00));
            order.setOrderAmount(BigDecimal.valueOf(7.00));
            order.setPlatformCommission(BigDecimal.ZERO);
            order.setEstimatedRunnerIncome(BigDecimal.valueOf(7.00));
            order.setContactName("李四");
            order.setContactPhone("13800001111");
            order.setPayStatus(2);
            order.setSettlementStatus(0);

            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            ErrandCategory category = buildCategory(1L, "食堂代购");
            when(errandCategoryMapper.selectById(1L)).thenReturn(category);

            // 状态日志
            OrderStatusLog log1 = buildStatusLog(1L, orderId, "ER20240101001",
                    null, 0, "CREATE_ORDER", 1L, "STUDENT", LocalDateTime.now().minusHours(2));
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(log1));

            // When: 管理员查看订单详情（管理员ID不使用，因为不走所有权校验）
            OrderDetailVO detail = adminOrderService.detail(orderId);

            // Then: 详情正常返回
            assertNotNull(detail, "订单详情不应为null");
            assertEquals(orderId, detail.getId(), "订单ID应一致");
            assertEquals("ER20240101001", detail.getOrderNo(), "订单编号应一致");
            assertEquals("食堂代购", detail.getCategoryName(), "分类名称应正确");
            // 管理员可以看到非本人订单
            assertEquals(Long.valueOf(1L), detail.getPublisherId(), "发布人ID应为1");
            assertEquals(Long.valueOf(2L), detail.getRunnerId(), "接单人ID应为2");

            // 验证联系方式仅出现在订单详情
            assertEquals("李四", detail.getContactName(), "联系人姓名应一致");
            assertEquals("13800001111", detail.getContactPhone(), "联系人手机号应一致");

            // 验证状态日志被返回
            assertNotNull(detail.getStatusLogs(), "状态流转日志不应为null");
            assertEquals(1, detail.getStatusLogs().size(), "应有1条状态日志");
        }

        /**
         * 测试管理员查看非本人订单详情成功
         * <p>
         * 这是管理员与普通用户的关键差异：
         * 普通用户调用 OrderService.detail 会校验所有权（必须为发布人或接单人），
         * 而管理员调用 AdminOrderService.detail 不应校验所有权，
         * 即使管理员既不等于 publisherId 也不等于 runnerId 也能查看。
         * </p>
         */
        @Test
        @DisplayName("管理员查看非本人订单应成功（所有权绕过）")
        void shouldAllowAdminToViewOtherPeoplesOrders() {
            // Given: 订单的发布人和接单人都不是管理员
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240101002", 10L, 20L, 1L, 3);
            order.setTitle("代寄快递");

            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            ErrandCategory category = buildCategory(1L, "快递寄件");
            when(errandCategoryMapper.selectById(1L)).thenReturn(category);

            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(new ArrayList<>());

            // When: 管理员查看此订单（管理员不传userId，不走所有权校验）
            OrderDetailVO detail = adminOrderService.detail(orderId);

            // Then: 成功返回详情，未被 ORDER_NOT_OWNED 拒绝
            assertNotNull(detail, "管理员应能查看非本人订单");
            assertEquals(orderId, detail.getId(), "订单ID应一致");
            assertEquals("代寄快递", detail.getTitle(), "订单标题应一致");
            // 验证所有权未被校验：publisherId 和 runnerId 都不是管理员，但仍可查看
            assertNotEquals(10L, 0L, "发布人不是本次查询者（管理层不传userId）");
        }

        /**
         * 测试订单不存在时抛出异常
         * <p>
         * 当 orderId 对应的订单不存在时，应抛出 BusinessException，
         * 错误码为 ORDER_NOT_FOUND(5001)。
         * </p>
         */
        @Test
        @DisplayName("订单不存在时应抛出 ORDER_NOT_FOUND 异常")
        void shouldThrowWhenOrderNotFound() {
            // Given: 订单不存在
            Long orderId = 999L;
            when(errandOrderMapper.selectById(orderId)).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminOrderService.detail(orderId),
                    "订单不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_FOUND(5001)");
            assertEquals(ErrorCode.ORDER_NOT_FOUND.getMessage(), exception.getMessage(),
                    "错误信息应为: 订单不存在");

            // 验证未执行后续查询
            verify(errandOrderMapper).selectById(orderId);
            verify(errandCategoryMapper, never()).selectById(anyLong());
            verify(orderStatusLogMapper, never()).selectList(any());
        }

        /**
         * 测试详情包含完整的状态流转日志
         * <p>
         * 验证管理员查看到的订单详情中，状态流转日志按创建时间升序排列，
         * 包含完整的日志信息。
         * </p>
         */
        @Test
        @DisplayName("订单详情应包含完整的状态流转日志")
        void shouldIncludeStatusLogsInDetail() {
            // Given: 订单有3条状态日志
            Long orderId = 300L;
            ErrandOrder order = buildOrder(orderId, "ER20240101003", 1L, 2L, 1L, 7);
            order.setTitle("已完成订单");

            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);

            OrderStatusLog log1 = buildStatusLog(1L, orderId, "ER20240101003",
                    null, 0, "CREATE_ORDER", 1L, "STUDENT", LocalDateTime.now().minusHours(5));
            OrderStatusLog log2 = buildStatusLog(2L, orderId, "ER20240101003",
                    0, 1, "PAY_SUCCESS", 0L, "SYSTEM", LocalDateTime.now().minusHours(4));
            OrderStatusLog log3 = buildStatusLog(3L, orderId, "ER20240101003",
                    1, 2, "ACCEPT_ORDER", 2L, "RUNNER", LocalDateTime.now().minusHours(3));
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(log1, log2, log3));

            // When: 管理员查看订单详情
            OrderDetailVO detail = adminOrderService.detail(orderId);

            // Then: 日志有3条，且按时间升序
            assertNotNull(detail.getStatusLogs(), "状态流转日志不应为null");
            assertEquals(3, detail.getStatusLogs().size(), "应有3条状态日志");

            // 验证日志顺序和内容
            assertEquals("CREATE_ORDER", detail.getStatusLogs().get(0).getTriggerAction(),
                    "第1条日志应为CREATE_ORDER");
            assertEquals("PAY_SUCCESS", detail.getStatusLogs().get(1).getTriggerAction(),
                    "第2条日志应为PAY_SUCCESS");
            assertEquals("ACCEPT_ORDER", detail.getStatusLogs().get(2).getTriggerAction(),
                    "第3条日志应为ACCEPT_ORDER");
        }

        /**
         * 管理员查订单详情应返回地址快照
         */
        @Test
        @DisplayName("管理员查订单详情应返回 pickupAddressDetail 和 deliveryAddressDetail")
        void shouldReturnAddressDetailsForAdmin() {
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER001", 1L, 2L, 1L, 2);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            // 地址快照
            ErrandOrderAddress pickup = new ErrandOrderAddress();
            pickup.setOrderId(orderId);
            pickup.setAddressRole(1);
            pickup.setDetailAddress("取件点地址");
            ErrandOrderAddress delivery = new ErrandOrderAddress();
            delivery.setOrderId(orderId);
            delivery.setAddressRole(2);
            delivery.setDetailAddress("送达点地址");
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(pickup, delivery));
            when(errandOrderDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            OrderDetailVO detail = adminOrderService.detail(orderId);

            assertNotNull(detail.getPickupAddressDetail(), "pickupAddressDetail 不应为 null");
            assertEquals("取件点地址", detail.getPickupAddressDetail().getDetailAddress());
            assertNotNull(detail.getDeliveryAddressDetail(), "deliveryAddressDetail 不应为 null");
            assertEquals("送达点地址", detail.getDeliveryAddressDetail().getDetailAddress());
            assertNull(detail.getOrderDetail(), "无数据时 orderDetail 应为 null");
        }

        /**
         * 管理员查订单详情地址快照缺失时不报错
         */
        @Test
        @DisplayName("管理员查订单详情地址快照缺失时不报错")
        void shouldNotThrowWhenNoAddressSnapshotForAdmin() {
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER001", 1L, 2L, 1L, 2);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            OrderDetailVO detail = adminOrderService.detail(orderId);

            assertNotNull(detail, "没地址快照时也应正常返回");
            assertNull(detail.getPickupAddressDetail(), "无快照时应为 null");
            assertNull(detail.getDeliveryAddressDetail(), "无快照时应为 null");
        }

        /**
         * 管理员查订单详情应返回分类扩展详情
         */
        @Test
        @DisplayName("管理员查订单详情应返回 orderDetail")
        void shouldReturnOrderDetailForAdmin() {
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER001", 1L, 2L, 1L, 2);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            ErrandOrderDetail mockDetail = new ErrandOrderDetail();
            mockDetail.setOrderId(orderId);
            mockDetail.setCategoryCode("EXPRESS_PICKUP");
            mockDetail.setExpressNo("SF123");
            when(errandOrderDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockDetail);

            OrderDetailVO detail = adminOrderService.detail(orderId);

            assertNotNull(detail.getOrderDetail(), "orderDetail 不应为 null");
            assertEquals("EXPRESS_PICKUP", detail.getOrderDetail().getCategoryCode());
            assertEquals("SF123", detail.getOrderDetail().getExpressNo());
        }

        /**
         * 管理员查订单详情分类详情缺失时不报错
         */
        @Test
        @DisplayName("管理员查订单详情分类详情缺失时不报错")
        void shouldNotThrowWhenNoOrderDetailForAdmin() {
            Long orderId = 100L;
            ErrandOrder order = buildOrder(orderId, "ER001", 1L, 2L, 1L, 2);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            OrderDetailVO detail = adminOrderService.detail(orderId);

            assertNotNull(detail, "没 detail 时也应正常返回");
            assertNull(detail.getOrderDetail(), "无数据时 orderDetail 应为 null");
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 构建测试用分类对象
     *
     * @param id           分类ID
     * @param categoryName 分类名称
     * @return 分类实体
     */
    private ErrandCategory buildCategory(Long id, String categoryName) {
        ErrandCategory category = new ErrandCategory();
        category.setId(id);
        category.setCategoryName(categoryName);
        category.setCategoryCode("CAT_" + id);
        category.setCategoryStatus(1);
        return category;
    }

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
        order.setEstimatedRunnerIncome(BigDecimal.valueOf(3.00));
        order.setOrderStatus(orderStatus);
        order.setPayStatus(0);
        order.setSettlementStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    /**
     * 构建测试用状态流转日志对象
     *
     * @param id            日志ID
     * @param orderId       订单ID
     * @param orderNo       订单编号
     * @param beforeStatus  变更前状态
     * @param afterStatus   变更后状态
     * @param triggerAction 触发动作
     * @param operatorUserId 操作人ID
     * @param operatorRole  操作人角色
     * @param createTime    创建时间
     * @return 状态流转日志实体
     */
    private OrderStatusLog buildStatusLog(Long id, Long orderId, String orderNo,
                                           Integer beforeStatus, Integer afterStatus,
                                           String triggerAction, Long operatorUserId,
                                           String operatorRole, LocalDateTime createTime) {
        OrderStatusLog log = new OrderStatusLog();
        log.setId(id);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setTriggerAction(triggerAction);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorRole(operatorRole);
        log.setCreateTime(createTime);
        return log;
    }
}
