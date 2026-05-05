package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.enums.RefundStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.OrderCancelRequest;
import com.example.backend.dto.request.OrderCreateRequest;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.ErrandOrderAddress;
import com.example.backend.entity.ErrandOrderDetail;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.entity.PaymentOrder;
import com.example.backend.entity.RefundRecord;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.RunnerIncomeRecord;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.ErrandOrderAddressMapper;
import com.example.backend.mapper.ErrandOrderDetailMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.mapper.RefundRecordMapper;
import com.example.backend.mapper.RunnerIncomeRecordMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.service.impl.OrderServiceImpl;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderStatusLogVO;
import com.example.backend.vo.OrderVO;
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
 * OrderService 单元测试类
 * <p>
 * 使用 Mockito 对 {@link OrderServiceImpl} 进行单元测试，
 * 覆盖订单创建（含费用计算与状态日志）、我的订单分页查询及订单详情查看三大核心业务流程。
 * <p>
 * 测试采用 @Nested 分组，将创建订单、订单列表和订单详情相关测试分别组织，
 * 每组包含正常场景和各类异常场景，确保业务逻辑的完整性验证。
 * <p>
 * Mock 策略：使用 {@code @Mock} 模拟所有 Mapper 层依赖，
 * 通过 {@code @InjectMocks} 将模拟对象注入被测 Service 实例，
 * 完全隔离数据库和 Spring 容器依赖。
 *
 * @author campus_running
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 单元测试")
class OrderServiceTest {

    // =========================================================================
    // Mock 依赖
    // =========================================================================

    /** Mock 跑腿订单 Mapper */
    @Mock
    private ErrandOrderMapper errandOrderMapper;

    /** Mock 任务分类 Mapper */
    @Mock
    private ErrandCategoryMapper errandCategoryMapper;

    /** Mock 订单状态流转日志 Mapper */
    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;

    /** Mock 订单评价 Mapper */
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;

    /** Mock 跑腿认证 Mapper */
    @Mock
    private RunnerAuthMapper runnerAuthMapper;

    /** Mock 退款记录 Mapper */
    @Mock
    private RefundRecordMapper refundRecordMapper;

    /** Mock 跑腿收益记录 Mapper */
    @Mock
    private RunnerIncomeRecordMapper runnerIncomeRecordMapper;

    /** Mock 订单地址快照 Mapper */
    @Mock
    private ErrandOrderAddressMapper errandOrderAddressMapper;

    /** Mock 订单分类扩展详情 Mapper */
    @Mock
    private ErrandOrderDetailMapper errandOrderDetailMapper;

    /** Mock 支付单 Mapper */
    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    /** 自动注入 Mock 依赖的被测对象 */
    @InjectMocks
    private OrderServiceImpl orderService;

    // =========================================================================
    // ArgumentCaptor
    // =========================================================================

    /** UpdateWrapper 捕获器 */
    @Captor
    private ArgumentCaptor<UpdateWrapper<ErrandOrder>> updateWrapperCaptor;

    /** OrderStatusLog 捕获器 */
    @Captor
    private ArgumentCaptor<OrderStatusLog> logCaptor;

    /** RefundRecord 捕获器 */
    @Captor
    private ArgumentCaptor<RefundRecord> refundRecordCaptor;

    /** ErrandOrderAddress 捕获器 */
    @Captor
    private ArgumentCaptor<ErrandOrderAddress> addressCaptor;

    /** ErrandOrderDetail 捕获器 */
    @Captor
    private ArgumentCaptor<ErrandOrderDetail> detailCaptor;

    // =========================================================================
    // 通用测试数据
    // =========================================================================

    /** 测试用用户ID（发布人） */
    private static final Long USER_ID = 1L;

    /** 测试用订单创建请求 */
    private OrderCreateRequest baseRequest;

    /** 测试用启用状态的分类 */
    private ErrandCategory enabledCategory;

    /**
     * 每个测试方法执行前的初始化操作
     * <p>
     * 构建通用的订单创建请求和启用状态分类对象，
     * 分类包含距离阶梯收费规则JSON，基础费用为3元。
     */
    @BeforeEach
    void setUp() {
        baseRequest = new OrderCreateRequest();
        baseRequest.setCategoryId(1L);
        baseRequest.setTitle("帮忙取快递");
        baseRequest.setOrderDesc("从菜鸟驿站取一个快递送到宿舍");
        baseRequest.setPickupAddress("菜鸟驿站");
        baseRequest.setDeliveryAddress("12号宿舍楼");
        baseRequest.setPickupLng(BigDecimal.valueOf(120.5123));
        baseRequest.setPickupLat(BigDecimal.valueOf(30.2345));
        baseRequest.setDeliveryLng(BigDecimal.valueOf(120.5156));
        baseRequest.setDeliveryLat(BigDecimal.valueOf(30.2378));
        baseRequest.setDistanceKm(BigDecimal.valueOf(2.5));
        baseRequest.setDeadlineTime(LocalDateTime.now().plusHours(2));

        enabledCategory = buildCategory(1L, "快递代取", BigDecimal.valueOf(3.00),
                "[{\"min\":0,\"max\":1,\"fee\":0},{\"min\":1,\"max\":3,\"fee\":2}]", 1);
    }

    // =========================================================================
    // 创建订单测试组
    // =========================================================================

    /**
     * 创建订单功能测试组
     * <p>
     * 覆盖订单创建的五种核心场景：
     * <ol>
     *   <li>正常创建成功 —— 分类存在且启用，距离在阶梯规则范围内</li>
     *   <li>分类不存在异常</li>
     *   <li>分类已停用异常</li>
     *   <li>无距离收费规则时使用默认费用（零）</li>
     *   <li>距离为null时使用第一条规则的费用</li>
     * </ol>
     */
    @Nested
    @DisplayName("创建订单")
    class CreateOrderTests {

        /**
         * 测试正常创建订单成功
         */
        @Test
        @DisplayName("应成功创建订单")
        void shouldCreateOrderSuccessfully() {
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> {
                ErrandOrder order = inv.getArgument(0);
                order.setId(100L);
                return 1;
            }).when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            Long orderId = orderService.create(USER_ID, baseRequest);

            assertEquals(Long.valueOf(100L), orderId, "应返回新创建的订单ID");
            verify(errandCategoryMapper).selectById(baseRequest.getCategoryId());

            ArgumentCaptor<ErrandOrder> orderCaptor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(orderCaptor.capture());
            ErrandOrder savedOrder = orderCaptor.getValue();

            assertNotNull(savedOrder.getOrderNo(), "订单编号不应为空");
            assertTrue(savedOrder.getOrderNo().startsWith("ER"), "订单编号应以ER开头");
            assertEquals(USER_ID, savedOrder.getPublisherId());
            assertNull(savedOrder.getRunnerId());
            assertEquals(baseRequest.getTitle(), savedOrder.getTitle());
            assertEquals(BigDecimal.valueOf(5).stripTrailingZeros(), savedOrder.getOrderAmount().stripTrailingZeros());
            assertEquals(Integer.valueOf(0), savedOrder.getOrderStatus());
            assertEquals(Integer.valueOf(0), savedOrder.getPayStatus());

            verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试分类不存在时抛出异常
         * <p>
         * 当 {@code errandCategoryMapper.selectById} 返回 null 时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#CATEGORY_NOT_FOUND}。
         */
        @Test
        @DisplayName("分类不存在时应抛出 CATEGORY_NOT_FOUND 异常")
        void shouldThrowWhenCategoryNotFound() {
            // Given: 分类不存在
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.create(USER_ID, baseRequest),
                    "分类不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 CATEGORY_NOT_FOUND(9001)");
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getMessage(), exception.getMessage(),
                    "错误信息应为: 分类不存在");

            // 验证未执行后续 insert 操作
            verify(errandOrderMapper, never()).insert(any(ErrandOrder.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试分类已停用时抛出异常
         * <p>
         * 当分类存在但 {@code categoryStatus} 不为 1（启用）时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#CATEGORY_NOT_FOUND}，
         * 并附带自定义错误信息 "该分类已停用"。
         */
        @Test
        @DisplayName("分类已停用时应抛出 CATEGORY_NOT_FOUND 异常")
        void shouldThrowWhenCategoryDisabled() {
            // Given: 分类存在但已停用（categoryStatus=2）
            ErrandCategory disabledCategory = buildCategory(1L, "已停用分类", BigDecimal.valueOf(3.00),
                    "[{\"min\":0,\"max\":1,\"fee\":0},{\"min\":1,\"max\":3,\"fee\":2}]", 2);
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(disabledCategory);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.create(USER_ID, baseRequest),
                    "分类已停用时应抛出 BusinessException");

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 CATEGORY_NOT_FOUND(9001)");
            assertEquals("该分类已停用", exception.getMessage(),
                    "错误信息应为: 该分类已停用");

            // 验证未执行后续 insert 操作
            verify(errandOrderMapper, never()).insert(any(ErrandOrder.class));
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试无距离收费规则时使用默认费用（零）
         * <p>
         * 当分类的 {@code distanceFeeRule} 为 null 时，距离费用应为 ZERO。
         * 验证即使分类有基础费用，没有距离规则时距离费用也为0。
         */
        @Test
        @DisplayName("无距离收费规则时应抛出 CATEGORY_INVALID_FEE_RULE 异常")
        void shouldThrowWhenNoDistanceRule() {
            ErrandCategory categoryNoRule = buildCategory(1L, "快递代取", BigDecimal.valueOf(3.00), null, 1);
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(categoryNoRule);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.create(USER_ID, baseRequest));

            assertEquals(ErrorCode.CATEGORY_INVALID_FEE_RULE.getCode(), exception.getCode());
        }

        /**
         * 测试距离为null时使用第一条规则的费用
         * <p>
         * 当请求中 {@code distanceKm} 为 null 时，应取第一条距离规则的fee值作为距离费用。
         * 第一条规则 {@code {"min":0,"max":1,"fee":0}} 费用为0。
         */
        @Test
        @DisplayName("距离为null时应抛出 BAD_REQUEST 异常")
        void shouldThrowWhenDistanceIsNull() {
            OrderCreateRequest nullDistanceRequest = new OrderCreateRequest();
            nullDistanceRequest.setCategoryId(1L);
            nullDistanceRequest.setTitle("测试任务");
            nullDistanceRequest.setOrderDesc("测试描述");
            nullDistanceRequest.setPickupAddress("取件点");
            nullDistanceRequest.setDeliveryAddress("送达点");
            nullDistanceRequest.setPickupLng(BigDecimal.valueOf(120.5));
            nullDistanceRequest.setPickupLat(BigDecimal.valueOf(30.2));
            nullDistanceRequest.setDeliveryLng(BigDecimal.valueOf(120.8));
            nullDistanceRequest.setDeliveryLat(BigDecimal.valueOf(30.5));
            nullDistanceRequest.setDistanceKm(null);
            nullDistanceRequest.setDeadlineTime(LocalDateTime.now().plusHours(1));

            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.create(USER_ID, nullDistanceRequest));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
        }

        /**
         * 创建订单时应写入两条地址快照
         */
        @Test
        @DisplayName("创建订单时应写入两条 ErrandOrderAddress")
        void shouldInsertTwoAddressSnapshots() {
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> {
                ErrandOrder order = inv.getArgument(0);
                order.setId(100L);
                return 1;
            }).when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            // 验证刚好 insert 了两次地址快照
            ArgumentCaptor<ErrandOrderAddress> captor = ArgumentCaptor.forClass(ErrandOrderAddress.class);
            verify(errandOrderAddressMapper, times(2)).insert(captor.capture());
            List<ErrandOrderAddress> addresses = captor.getAllValues();

            // 起点地址
            ErrandOrderAddress pickup = addresses.get(0);
            assertEquals(Long.valueOf(100L), pickup.getOrderId());
            assertEquals(Integer.valueOf(1), pickup.getAddressRole(), "起点 addressRole 应为1");
            assertEquals(baseRequest.getPickupAddress(), pickup.getDetailAddress());
            assertEquals(baseRequest.getPickupLng(), pickup.getLongitude());
            assertEquals(baseRequest.getPickupLat(), pickup.getLatitude());
            assertEquals(Integer.valueOf(9), pickup.getMapProvider(), "mapProvider 应为9（系统兜底）");
            assertEquals(Integer.valueOf(1), pickup.getAddressSource(), "未传时默认 1");
            assertEquals("GCJ02", pickup.getCoordType());

            // 终点地址
            ErrandOrderAddress delivery = addresses.get(1);
            assertEquals(Long.valueOf(100L), delivery.getOrderId());
            assertEquals(Integer.valueOf(2), delivery.getAddressRole(), "终点 addressRole 应为2");
            assertEquals(baseRequest.getDeliveryAddress(), delivery.getDetailAddress());
            assertEquals(baseRequest.getDeliveryLng(), delivery.getLongitude());
            assertEquals(baseRequest.getDeliveryLat(), delivery.getLatitude());
        }

        /**
         * 老请求只传基本字段时也能写入地址快照
         */
        @Test
        @DisplayName("老请求只传基本字段时也能写入地址快照")
        void shouldInsertAddressSnapshotWithLegacyRequest() {
            OrderCreateRequest legacyRequest = new OrderCreateRequest();
            legacyRequest.setCategoryId(1L);
            legacyRequest.setTitle("测试");
            legacyRequest.setOrderDesc("测试描述");
            legacyRequest.setPickupAddress("取件点");
            legacyRequest.setDeliveryAddress("送达点");
            legacyRequest.setPickupLng(BigDecimal.valueOf(120.5));
            legacyRequest.setPickupLat(BigDecimal.valueOf(30.2));
            legacyRequest.setDeliveryLng(null);
            legacyRequest.setDeliveryLat(null);
            legacyRequest.setDistanceKm(BigDecimal.valueOf(1.0));
            legacyRequest.setDeadlineTime(LocalDateTime.now().plusHours(1));

            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
            doAnswer(inv -> {
                ErrandOrder order = inv.getArgument(0);
                order.setId(200L);
                return 1;
            }).when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);

            orderService.create(USER_ID, legacyRequest);

            ArgumentCaptor<ErrandOrderAddress> captor = ArgumentCaptor.forClass(ErrandOrderAddress.class);
            verify(errandOrderAddressMapper, times(2)).insert(captor.capture());
            List<ErrandOrderAddress> addresses = captor.getAllValues();

            // 终点无经纬度 → geocodeStatus=0
            ErrandOrderAddress delivery = addresses.get(1);
            assertEquals(Integer.valueOf(0), delivery.getGeocodeStatus(), "无经纬度时 geocodeStatus 应为0");
            assertNull(delivery.getGeocodeTime(), "无经纬度时 geocodeTime 应为 null");
        }

        /**
         * 经纬度完整时 geocodeStatus=1
         */
        @Test
        @DisplayName("经纬度完整时 geocodeStatus=1, geocodeTime 非空")
        void shouldSetGeocodeStatusWhenCoordsComplete() {
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> {
                ErrandOrder order = inv.getArgument(0);
                order.setId(100L);
                return 1;
            }).when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrderAddress> captor = ArgumentCaptor.forClass(ErrandOrderAddress.class);
            verify(errandOrderAddressMapper, times(2)).insert(captor.capture());

            // baseRequest 两个经纬度都有值
            ErrandOrderAddress pickup = captor.getAllValues().get(0);
            assertEquals(Integer.valueOf(1), pickup.getGeocodeStatus(), "经纬度完整时 geocodeStatus 应为1");
            assertNotNull(pickup.getGeocodeTime(), "经纬度完整时 geocodeTime 不应为 null");
        }

        /**
         * 创建订单时插入 1 条 ErrandOrderDetail，categoryCode 来自 ErrandCategory
         */
        @Test
        @DisplayName("创建订单时应插入 ErrandOrderDetail，categoryCode 来自分类")
        void shouldInsertOrderDetailWithCategoryCode() {
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrderDetail> captor = ArgumentCaptor.forClass(ErrandOrderDetail.class);
            verify(errandOrderDetailMapper).insert(captor.capture());
            ErrandOrderDetail detail = captor.getValue();

            assertEquals(Long.valueOf(100L), detail.getOrderId());
            // categoryCode 必须来自分类实体，不是请求参数
            assertEquals(enabledCategory.getCategoryCode(), detail.getCategoryCode());
            // 默认值
            assertEquals(Integer.valueOf(1), detail.getPackageCount(), "默认 packageCount=1");
            assertEquals(Integer.valueOf(0), detail.getNeedInsulation(), "默认 needInsulation=0");
            assertEquals(Integer.valueOf(0), detail.getAllowPriceAdjust(), "默认 allowPriceAdjust=0");
        }

        /**
         * 老请求不传扩展字段时也能插入 detail，默认值正确
         */
        @Test
        @DisplayName("老请求不传扩展字段时也插入 detail，默认值正确")
        void shouldInsertDetailWithDefaultValuesForLegacyRequest() {
            OrderCreateRequest legacyReq = new OrderCreateRequest();
            legacyReq.setCategoryId(1L);
            legacyReq.setTitle("测试");
            legacyReq.setOrderDesc("测试");
            legacyReq.setPickupAddress("A");
            legacyReq.setDeliveryAddress("B");
            legacyReq.setPickupLng(new BigDecimal("120.5"));
            legacyReq.setPickupLat(new BigDecimal("30.2"));
            legacyReq.setDeliveryLng(new BigDecimal("120.8"));
            legacyReq.setDeliveryLat(new BigDecimal("30.5"));
            legacyReq.setDistanceKm(BigDecimal.ONE);
            legacyReq.setDeadlineTime(LocalDateTime.now().plusHours(1));

            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(200L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, legacyReq);

            ArgumentCaptor<ErrandOrderDetail> captor = ArgumentCaptor.forClass(ErrandOrderDetail.class);
            verify(errandOrderDetailMapper).insert(captor.capture());
            ErrandOrderDetail detail = captor.getValue();

            assertEquals(Integer.valueOf(1), detail.getPackageCount(), "默认 1");
            assertEquals(Integer.valueOf(0), detail.getNeedInsulation(), "默认 0");
            assertEquals(Integer.valueOf(0), detail.getAllowPriceAdjust(), "默认 0");
            assertNull(detail.getExpressNo(), "未传时应为 null");
            assertNull(detail.getTakeawayPlatform(), "未传时应为 null");
            assertNull(detail.getShoppingItems(), "未传时应为 null");
        }

        /**
         * 快递字段能正确写入
         */
        @Test
        @DisplayName("快递字段能正确写入 ErrandOrderDetail")
        void shouldInsertExpressFields() {
            baseRequest.setExpressCompany("顺丰快递");
            baseRequest.setExpressNo("SF1234567890");
            baseRequest.setExpressPickupCode("8888");
            baseRequest.setPackageCount(2);

            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrderDetail> captor = ArgumentCaptor.forClass(ErrandOrderDetail.class);
            verify(errandOrderDetailMapper).insert(captor.capture());
            ErrandOrderDetail detail = captor.getValue();

            assertEquals("顺丰快递", detail.getExpressCompany());
            assertEquals("SF1234567890", detail.getExpressNo());
            assertEquals("8888", detail.getExpressPickupCode());
            assertEquals(Integer.valueOf(2), detail.getPackageCount());
        }

        /**
         * 外卖字段能正确写入
         */
        @Test
        @DisplayName("外卖字段能正确写入 ErrandOrderDetail")
        void shouldInsertTakeawayFields() {
            baseRequest.setTakeawayPlatform("MEITUAN");
            baseRequest.setMerchantName("麦当劳");
            LocalDateTime pickupTime = LocalDateTime.now().plusMinutes(30);
            baseRequest.setExpectedPickupTime(pickupTime);

            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrderDetail> captor = ArgumentCaptor.forClass(ErrandOrderDetail.class);
            verify(errandOrderDetailMapper).insert(captor.capture());
            ErrandOrderDetail detail = captor.getValue();

            assertEquals("MEITUAN", detail.getTakeawayPlatform());
            assertEquals("麦当劳", detail.getMerchantName());
            assertEquals(pickupTime, detail.getExpectedPickupTime());
        }

        /**
         * 代买字段能正确写入
         */
        @Test
        @DisplayName("代买字段能正确写入 ErrandOrderDetail")
        void shouldInsertShoppingFields() {
            baseRequest.setShoppingItems("[{\"name\":\"可乐\",\"qty\":2}]");
            baseRequest.setShoppingBudget(new BigDecimal("50.00"));
            baseRequest.setAllowPriceAdjust(1);

            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrderDetail> captor = ArgumentCaptor.forClass(ErrandOrderDetail.class);
            verify(errandOrderDetailMapper).insert(captor.capture());
            ErrandOrderDetail detail = captor.getValue();

            assertEquals("[{\"name\":\"可乐\",\"qty\":2}]", detail.getShoppingItems());
            assertEquals(new BigDecimal("50.00"), detail.getShoppingBudget());
            assertEquals(Integer.valueOf(1), detail.getAllowPriceAdjust());
        }

        /**
         * create() 应写入 feeRuleVersion 从分类获取
         */
        @Test
        @DisplayName("create() 应写入 feeRuleVersion")
        void shouldSetFeeRuleVersionFromCategory() {
            enabledCategory.setFeeRuleVersion("v2");
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrder> captor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(captor.capture());
            assertEquals("v2", captor.getValue().getFeeRuleVersion());
        }

        /**
         * create() 应写入 feeDetail JSON
         */
        @Test
        @DisplayName("create() 应写入 feeDetail JSON 包含费用字段")
        void shouldWriteFeeDetailJson() {
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrder> captor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(captor.capture());
            String detail = captor.getValue().getFeeDetail();
            assertNotNull(detail, "feeDetail 不应为空");
            assertTrue(detail.contains("\"baseFee\""), "应包含 baseFee");
            assertTrue(detail.contains("\"distanceFee\""), "应包含 distanceFee");
            assertTrue(detail.contains("\"orderAmount\""), "应包含 orderAmount");
            assertTrue(detail.contains("\"feeRuleVersion\""), "应包含 feeRuleVersion");
        }

        /**
         * create() 应写入 attachmentUrls/contactName/contactPhone
         */
        @Test
        @DisplayName("create() 应写入 attachmentUrls/contactName/contactPhone")
        void shouldWriteAttachmentAndContact() {
            baseRequest.setAttachmentUrls("http://img1.jpg,http://img2.jpg");
            baseRequest.setContactName("张三");
            baseRequest.setContactPhone("13900000001");
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrder> captor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(captor.capture());
            ErrandOrder o = captor.getValue();
            assertEquals("http://img1.jpg,http://img2.jpg", o.getAttachmentUrls());
            assertEquals("张三", o.getContactName());
            assertEquals("13900000001", o.getContactPhone());
        }

        /**
         * contactName 为空时兜底到 pickupContactName
         */
        @Test
        @DisplayName("contactName 为空时兜底到 pickupContactName")
        void shouldFallbackContactName() {
            baseRequest.setContactName(null);
            baseRequest.setPickupContactName("李四");
            baseRequest.setContactPhone(null);
            baseRequest.setPickupContactPhone("13900000002");
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrder> captor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(captor.capture());
            ErrandOrder o = captor.getValue();
            assertEquals("李四", o.getContactName(), "应兜底到 pickupContactName");
            assertEquals("13900000002", o.getContactPhone(), "应兜底到 pickupContactPhone");
        }

        /**
         * 经纬度完整时 distance 兜底字段正确
         */
        @Test
        @DisplayName("经纬度完整时应设置 distance 字段")
        void shouldSetDistanceFieldsWhenCoordsComplete() {
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrder> captor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(captor.capture());
            ErrandOrder o = captor.getValue();
            assertEquals(baseRequest.getDistanceKm(), o.getStraightDistanceKm());
            assertEquals(Integer.valueOf(2), o.getDistanceSource(), "不接地图时应为2（直线兜底）");
            assertEquals(Integer.valueOf(1), o.getDistanceCalcStatus(), "坐标完整应为 1");
            assertEquals(Integer.valueOf(9), o.getMapProvider());
            assertNotNull(o.getDistanceCalcTime(), "坐标完整时不应为 null");
            assertNull(o.getRouteDistanceKm(), "不接地图时 routeDistanceKm 应为 null");
        }

        /**
         * 经纬度不完整时 distance calc 字段为 0/null
         */
        @Test
        @DisplayName("经纬度不完整时 distanceCalcStatus=0")
        void shouldSetCalcStatusZeroWhenCoordsIncomplete() {
            baseRequest.setDeliveryLng(null); // 使坐标不完整
            when(errandCategoryMapper.selectById(baseRequest.getCategoryId())).thenReturn(enabledCategory);
            doAnswer(inv -> { ErrandOrder o = inv.getArgument(0); o.setId(100L); return 1; })
                    .when(errandOrderMapper).insert(any(ErrandOrder.class));
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(errandOrderAddressMapper.insert(any(ErrandOrderAddress.class))).thenReturn(1);
            when(errandOrderDetailMapper.insert(any(ErrandOrderDetail.class))).thenReturn(1);

            orderService.create(USER_ID, baseRequest);

            ArgumentCaptor<ErrandOrder> captor = ArgumentCaptor.forClass(ErrandOrder.class);
            verify(errandOrderMapper).insert(captor.capture());
            ErrandOrder o = captor.getValue();
            assertEquals(Integer.valueOf(0), o.getDistanceCalcStatus(), "坐标不完整应为 0");
            assertNull(o.getDistanceCalcTime(), "坐标不完整时应为 null");
        }
    }

    // =========================================================================
    // 我的订单测试组
    // =========================================================================

    /**
     * 我的订单功能测试组
     * <p>
     * 覆盖分页查询的三种核心场景：
     * <ol>
     *   <li>查询用户关联订单 —— 作为发布人或接单人关联的订单全部返回</li>
     *   <li>按订单状态筛选</li>
     *   <li>无关联订单时返回空页</li>
     * </ol>
     */
    @Nested
    @DisplayName("我的订单")
    class MyOrdersTests {

        /**
         * 测试返回用户关联的订单列表
         * <p>
         * 验证点：
         * <ul>
         *   <li>用户作为发布人的订单被返回</li>
         *   <li>用户作为接单人的订单被返回</li>
         *   <li>每个订单的分类名称正确填充</li>
         *   <li>分页信息（总数、每页大小）正确</li>
         * </ul>
         */
        @Test
        @DisplayName("应返回用户作为发布人或接单人的订单列表")
        void shouldReturnMyOrders() {
            // Given: 用户有一个作为发布人的订单，一个作为接单人的订单
            ErrandOrder orderAsPublisher = buildOrder(1L, "ER20240101001", USER_ID, null, 1L, 0);
            ErrandOrder orderAsRunner = buildOrder(2L, "ER20240101002", 2L, USER_ID, 2L, 1);
            List<ErrandOrder> records = Arrays.asList(orderAsPublisher, orderAsRunner);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(records);
                        page.setTotal(records.size());
                        return page;
                    });

            ErrandCategory cat1 = buildCategory(1L, "快递代取", BigDecimal.valueOf(3.00), null, 1);
            ErrandCategory cat2 = buildCategory(2L, "食堂代购", BigDecimal.valueOf(5.00), null, 1);
            when(errandCategoryMapper.selectById(1L)).thenReturn(cat1);
            when(errandCategoryMapper.selectById(2L)).thenReturn(cat2);

            // When: 查询我的订单（不筛选状态）
            IPage<OrderVO> result = orderService.myOrders(USER_ID, null, null, 1, 10);

            // Then: 验证分页信息
            assertNotNull(result, "返回结果不应为null");
            assertEquals(2, result.getTotal(), "总数应为2");
            assertEquals(2, result.getRecords().size(), "记录数应为2");

            // 验证第一条记录（发布人订单）
            OrderVO vo1 = result.getRecords().get(0);
            assertEquals(Long.valueOf(1L), vo1.getId(), "订单ID应一致");
            assertEquals("ER20240101001", vo1.getOrderNo(), "订单编号应一致");
            assertEquals(USER_ID, vo1.getPublisherId(), "发布人ID应一致");
            assertNull(vo1.getRunnerId(), "该订单接单人应为null");
            assertEquals("快递代取", vo1.getCategoryName(), "分类名称应正确");

            // 验证第二条记录（接单人订单）
            OrderVO vo2 = result.getRecords().get(1);
            assertEquals(Long.valueOf(2L), vo2.getId(), "订单ID应一致");
            assertEquals("ER20240101002", vo2.getOrderNo(), "订单编号应一致");
            assertEquals(Long.valueOf(2L), vo2.getPublisherId(), "发布人ID应为2");
            assertEquals(USER_ID, vo2.getRunnerId(), "接单人ID应为当前用户");
            assertEquals("食堂代购", vo2.getCategoryName(), "分类名称应正确");

            // 验证 selectPage 被调用
            verify(errandOrderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        /**
         * 测试按订单状态和支付状态筛选
         * <p>
         * 当传入 {@code orderStatus} 和 {@code payStatus} 参数时，仅返回状态匹配的订单。
         * 由于底层查询由 MyBatis-Plus 执行，Mock 层直接返回筛选后的数据，
         * 测试验证分页结果中的记录均符合筛选条件。
         * </p>
         */
        @Test
        @DisplayName("应按订单状态和支付状态筛选并仅返回匹配的订单")
        void shouldFilterByOrderStatus() {
            // Given: 返回仅含状态为0（UNPAID）且支付状态为0（UNPAID）的订单
            ErrandOrder unpaidOrder = buildOrder(1L, "ER20240101003", USER_ID, null, 1L, 0);
            unpaidOrder.setPayStatus(PayStatusEnum.UNPAID.getCode());
            // buildOrder 第6个参数 orderStatus=0
            List<ErrandOrder> filteredRecords = Collections.singletonList(unpaidOrder);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(filteredRecords);
                        page.setTotal(filteredRecords.size());
                        return page;
                    });

            ErrandCategory category = buildCategory(1L, "快递代取", BigDecimal.valueOf(3.00), null, 1);
            when(errandCategoryMapper.selectById(1L)).thenReturn(category);

            // When: 查询状态为0且支付状态也为0的订单
            IPage<OrderVO> result = orderService.myOrders(USER_ID,
                    OrderStatusEnum.UNPAID.getCode(),
                    PayStatusEnum.UNPAID.getCode(),
                    1, 10);

            // Then: 仅返回匹配状态的订单
            assertNotNull(result, "返回结果不应为null");
            assertEquals(1, result.getTotal(), "筛选后总数应为1");
            assertEquals(1, result.getRecords().size(), "筛选后记录数应为1");
            assertEquals(Integer.valueOf(0), result.getRecords().get(0).getOrderStatus(),
                    "返回订单的订单状态应为0(UNPAID)");
            assertEquals(Integer.valueOf(PayStatusEnum.UNPAID.getCode()), result.getRecords().get(0).getPayStatus(),
                    "返回订单的支付状态应为0(UNPAID)");

            // 验证 selectPage 被调用
            verify(errandOrderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        /**
         * 测试无关联订单时返回空页
         * <p>
         * 当用户没有任何发布或接单记录时，应返回空的分页结果。
         * 验证 total 为 0，records 为空列表。
         * </p>
         */
        @Test
        @DisplayName("无关联订单时应返回空页")
        void shouldReturnEmptyPage() {
            // Given: 无任何关联订单
            List<ErrandOrder> emptyRecords = new ArrayList<>();

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<ErrandOrder> page = inv.getArgument(0);
                        page.setRecords(emptyRecords);
                        page.setTotal(0L);
                        return page;
                    });

            // When: 查询我的订单
            IPage<OrderVO> result = orderService.myOrders(USER_ID, null, null, 1, 10);

            // Then: 返回空分页
            assertNotNull(result, "返回结果不应为null");
            assertEquals(0, result.getTotal(), "总数应为0");
            assertTrue(result.getRecords().isEmpty(), "记录列表应为空");

            // 验证 selectPage 被调用
            verify(errandOrderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
            // 分类查询不应被调用
            verify(errandCategoryMapper, never()).selectById(anyLong());
        }
    }

    // =========================================================================
    // 订单详情测试组
    // =========================================================================

    /**
     * 订单详情功能测试组
     * <p>
     * 覆盖订单详情的四种核心场景：
     * <ol>
     *   <li>正常查询详情 —— 订单存在且用户为所属人，含状态流转日志</li>
     *   <li>订单不存在异常</li>
     *   <li>用户非订单所属人异常</li>
     *   <li>接单人也可查看订单详情 —— 确保非发布人但为接单人的用户也能查看</li>
     * </ol>
     */
    @Nested
    @DisplayName("订单详情")
    class DetailTests {

        /**
         * 测试正常返回订单详情（含状态流转日志）
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单查询成功</li>
         *   <li>发布人匹配所有权校验通过</li>
         *   <li>分类名称正确填充</li>
         *   <li>状态流转日志按时间升序返回</li>
         *   <li>返回的 {@link OrderDetailVO} 包含所有订单字段和日志列表</li>
         * </ul>
         */
        @Test
        @DisplayName("应返回订单详情及状态流转日志")
        void shouldReturnDetailWithStatusLogs() {
            // Given: 订单存在，用户为发布人
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240102001", USER_ID, null, 1L, 1);
            order.setTitle("帮忙取快递");
            order.setOrderDesc("测试订单描述");
            order.setPickupAddress("取件点A");
            order.setDeliveryAddress("送达点B");
            order.setPickupLng(BigDecimal.valueOf(120.5));
            order.setPickupLat(BigDecimal.valueOf(30.2));
            order.setDeliveryLng(BigDecimal.valueOf(120.8));
            order.setDeliveryLat(BigDecimal.valueOf(30.5));
            order.setDistanceKm(BigDecimal.valueOf(2.5));
            order.setBaseFee(BigDecimal.valueOf(3.00));
            order.setDistanceFee(BigDecimal.valueOf(2));
            order.setWeightFee(BigDecimal.ZERO);
            order.setTimeFee(BigDecimal.ZERO);
            order.setTipFee(BigDecimal.ZERO);
            order.setOrderAmount(BigDecimal.valueOf(5));
            order.setPlatformCommission(BigDecimal.ZERO);
            order.setEstimatedRunnerIncome(BigDecimal.valueOf(5));
            order.setPayStatus(0);
            order.setSettlementStatus(0);
            order.setContactName("张三");
            order.setContactPhone("13900000001");
            order.setDeadlineTime(LocalDateTime.now().plusHours(2));

            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // 分类查询
            ErrandCategory category = buildCategory(1L, "快递代取", BigDecimal.valueOf(3.00), null, 1);
            when(errandCategoryMapper.selectById(1L)).thenReturn(category);

            // 状态流转日志
            OrderStatusLog log1 = buildStatusLog(1L, orderId, "ER20240102001",
                    null, 0, "CREATE_ORDER", USER_ID, "STUDENT", LocalDateTime.now().minusHours(2));
            OrderStatusLog log2 = buildStatusLog(2L, orderId, "ER20240102001",
                    0, 1, "PAY_SUCCESS", 0L, "SYSTEM", LocalDateTime.now().minusHours(1));
            List<OrderStatusLog> logs = Arrays.asList(log1, log2);

            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(logs);

            // When: 查询订单详情
            OrderDetailVO detail = orderService.detail(orderId, USER_ID);

            // Then: 验证详情字段
            assertNotNull(detail, "订单详情不应为null");
            assertEquals(orderId, detail.getId(), "订单ID应一致");
            assertEquals("ER20240102001", detail.getOrderNo(), "订单编号应一致");
            assertEquals(USER_ID, detail.getPublisherId(), "发布人ID应一致");
            assertNull(detail.getRunnerId(), "接单人ID应为null");
            assertEquals(Long.valueOf(1L), detail.getCategoryId(), "分类ID应一致");
            assertEquals("快递代取", detail.getCategoryName(), "分类名称应正确");
            assertEquals("帮忙取快递", detail.getTitle(), "标题应一致");
            assertEquals(Integer.valueOf(1), detail.getOrderStatus(), "订单状态应一致");

            // 验证联系方式仅出现在订单详情，不在列表 VO 中
            assertEquals("张三", detail.getContactName(), "联系人姓名应一致");
            assertEquals("13900000001", detail.getContactPhone(), "联系人手机号应一致");

            // 验证状态流转日志
            assertNotNull(detail.getStatusLogs(), "状态流转日志列表不应为null");
            assertEquals(2, detail.getStatusLogs().size(), "应有2条状态日志");

            // 验证第一条日志（创建订单）
            OrderStatusLogVO logVO1 = detail.getStatusLogs().get(0);
            assertEquals(Long.valueOf(1L), logVO1.getId(), "日志ID应一致");
            assertEquals(orderId, logVO1.getOrderId(), "日志关联订单ID应一致");
            assertNull(logVO1.getBeforeStatus(), "变更前状态应为null");
            assertEquals(Integer.valueOf(0), logVO1.getAfterStatus(), "变更后状态应为UNPAID(0)");
            assertEquals("CREATE_ORDER", logVO1.getTriggerAction(), "触发动作应为CREATE_ORDER");
            assertEquals(USER_ID, logVO1.getOperatorUserId(), "操作人ID应为发布人");
            assertEquals("STUDENT", logVO1.getOperatorRole(), "操作人角色应为STUDENT");

            // 验证第二条日志（支付成功）
            OrderStatusLogVO logVO2 = detail.getStatusLogs().get(1);
            assertEquals(Long.valueOf(2L), logVO2.getId(), "日志ID应一致");
            assertEquals(Integer.valueOf(0), logVO2.getBeforeStatus(), "变更前状态应为UNPAID(0)");
            assertEquals(Integer.valueOf(1), logVO2.getAfterStatus(), "变更后状态应为WAITING_ACCEPT(1)");
            assertEquals("PAY_SUCCESS", logVO2.getTriggerAction(), "触发动作应为PAY_SUCCESS");
            assertEquals(Long.valueOf(0L), logVO2.getOperatorUserId(), "操作人ID应为0（系统）");
            assertEquals("SYSTEM", logVO2.getOperatorRole(), "操作人角色应为SYSTEM");

            // 验证 Mapper 调用
            verify(errandOrderMapper).selectById(orderId);
            verify(errandCategoryMapper).selectById(1L);
            verify(orderStatusLogMapper).selectList(any(LambdaQueryWrapper.class));
        }

        /**
         * 测试订单不存在时抛出异常
         * <p>
         * 当 {@code errandOrderMapper.selectById} 返回 null 时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#ORDER_NOT_FOUND}。
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
                    () -> orderService.detail(orderId, USER_ID),
                    "订单不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_FOUND(5001)");
            assertEquals(ErrorCode.ORDER_NOT_FOUND.getMessage(), exception.getMessage(),
                    "错误信息应为: 订单不存在");

            // 验证未执行后续查询
            verify(errandOrderMapper).selectById(orderId);
            verify(errandCategoryMapper, never()).selectById(anyLong());
            verify(orderStatusLogMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }

        /**
         * 测试用户非订单所属人时抛出异常
         * <p>
         * 当订单的 {@code publisherId} 和 {@code runnerId} 均不等于当前用户ID时，
         * 应抛出 {@link BusinessException} 且错误码为 {@link ErrorCode#ORDER_NOT_OWNED}。
         * </p>
         */
        @Test
        @DisplayName("用户非所属人时应抛出 ORDER_NOT_OWNED 异常")
        void shouldThrowWhenNotOwner() {
            // Given: 订单存在但发布人和接单人均不是当前用户
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240102002", 2L, 3L, 1L, 1);
            // publisherId=2, runnerId=3，USER_ID=1 都不是

            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.detail(orderId, USER_ID),
                    "用户非所属人时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_OWNED.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_OWNED(5002)");
            assertEquals(ErrorCode.ORDER_NOT_OWNED.getMessage(), exception.getMessage(),
                    "错误信息应为: 无权操作该订单");

            // 验证未执行后续查询
            verify(errandOrderMapper).selectById(orderId);
            verify(errandCategoryMapper, never()).selectById(anyLong());
            verify(orderStatusLogMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }

        /**
         * 测试用户作为接单人（而非发布人）也可以查看订单详情
         * <p>
         * 当用户是订单的接单人（runnerId）时，所有权校验也应通过。
         * 此额外测试确保发布人和接单人都能查看订单详情。
         * </p>
         */
        @Test
        @DisplayName("接单人也可查看订单详情")
        void shouldAllowRunnerToViewDetail() {
            // Given: 用户是接单人（runnerId=USER_ID），发布人是其他人
            Long orderId = 300L;
            ErrandOrder order = buildOrder(orderId, "ER20240103001", 5L, USER_ID, 1L, 2);
            order.setTitle("代取文件");

            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            ErrandCategory category = buildCategory(1L, "文件代取", BigDecimal.valueOf(5.00), null, 1);
            when(errandCategoryMapper.selectById(1L)).thenReturn(category);

            List<OrderStatusLog> logs = new ArrayList<>();
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(logs);

            // When: 接单人查看订单详情
            OrderDetailVO detail = orderService.detail(orderId, USER_ID);

            // Then: 验证成功返回详情
            assertNotNull(detail, "接单人应能成功查看订单详情");
            assertEquals(orderId, detail.getId(), "订单ID应一致");
            assertEquals(USER_ID, detail.getRunnerId(), "接单人ID应为当前用户");
            assertEquals("文件代取", detail.getCategoryName(), "分类名称应正确");
            assertTrue(detail.getStatusLogs().isEmpty(), "状态日志应为空列表");

            // 验证所有权校验通过后执行了完整查询
            verify(errandOrderMapper).selectById(orderId);
            verify(errandCategoryMapper).selectById(1L);
            verify(orderStatusLogMapper).selectList(any(LambdaQueryWrapper.class));
        }

        /**
         * detail() 查询到地址快照时返回 pickupAddressDetail 和 deliveryAddressDetail
         */
        @Test
        @DisplayName("detail() 查询到地址快照时应返回地址详情")
        void shouldReturnAddressDetails() {
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240102001", USER_ID, null, 1L, 1);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            // 模拟返回两条地址快照
            ErrandOrderAddress pickupAddr = new ErrandOrderAddress();
            pickupAddr.setOrderId(orderId);
            pickupAddr.setAddressRole(1);
            pickupAddr.setDetailAddress("取件点地址");

            ErrandOrderAddress deliveryAddr = new ErrandOrderAddress();
            deliveryAddr.setOrderId(orderId);
            deliveryAddr.setAddressRole(2);
            deliveryAddr.setDetailAddress("送达点地址");

            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(pickupAddr, deliveryAddr));

            OrderDetailVO detail = orderService.detail(orderId, USER_ID);

            assertNotNull(detail.getPickupAddressDetail(), "pickupAddressDetail 不应为 null");
            assertEquals("取件点地址", detail.getPickupAddressDetail().getDetailAddress());
            assertNotNull(detail.getDeliveryAddressDetail(), "deliveryAddressDetail 不应为 null");
            assertEquals("送达点地址", detail.getDeliveryAddressDetail().getDetailAddress());
        }

        /**
         * detail() 查询到 ErrandOrderDetail 时返回 orderDetail
         */
        @Test
        @DisplayName("detail() 查询到 ErrandOrderDetail 时返回 orderDetail")
        void shouldReturnOrderDetail() {
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240102001", USER_ID, null, 1L, 1);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            ErrandOrderDetail mockDetail = new ErrandOrderDetail();
            mockDetail.setOrderId(orderId);
            mockDetail.setCategoryCode("EXPRESS_PICKUP");
            mockDetail.setExpressNo("SF123456");
            when(errandOrderDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockDetail);

            OrderDetailVO detail = orderService.detail(orderId, USER_ID);

            assertNotNull(detail.getOrderDetail(), "orderDetail 不应为 null");
            assertEquals("EXPRESS_PICKUP", detail.getOrderDetail().getCategoryCode());
            assertEquals("SF123456", detail.getOrderDetail().getExpressNo());
        }

        /**
         * detail() 查不到 ErrandOrderDetail 时仍正常返回
         */
        @Test
        @DisplayName("detail() 查不到 ErrandOrderDetail 时仍正常返回")
        void shouldNotThrowWhenNoOrderDetail() {
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240102001", USER_ID, null, 1L, 1);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            when(errandOrderDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            OrderDetailVO detail = orderService.detail(orderId, USER_ID);

            assertNotNull(detail, "没 detail 时也应正常返回");
            assertNull(detail.getOrderDetail(), "无数据时 orderDetail 应为 null");
        }

        /**
         * detail() 查不到地址快照时仍正常返回，不报错
         */
        @Test
        @DisplayName("detail() 查不到地址快照时仍正常返回")
        void shouldNotThrowWhenNoAddressSnapshot() {
            Long orderId = 200L;
            ErrandOrder order = buildOrder(orderId, "ER20240102001", USER_ID, null, 1L, 1);
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            when(errandCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
            when(orderStatusLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
            // 地址快照返回空列表
            when(errandOrderAddressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(new ArrayList<>());

            OrderDetailVO detail = orderService.detail(orderId, USER_ID);

            assertNotNull(detail, "没地址快照时也应正常返回订单详情");
            assertNull(detail.getPickupAddressDetail(), "无快照时 pickupAddressDetail 应为 null");
            assertNull(detail.getDeliveryAddressDetail(), "无快照时 deliveryAddressDetail 应为 null");
        }
    }

    // =========================================================================
    // 任务大厅测试组
    // =========================================================================

    /**
     * 任务大厅功能测试组
     * <p>
     * 覆盖任务大厅的三种核心场景：
     * <ol>
     *   <li>已认证跑腿员查询大厅成功 —— 返回符合条件的订单列表</li>
     *   <li>未认证用户查询大厅失败 —— 抛出 ORDER_HALL_ACCESS_DENIED 异常</li>
     *   <li>大厅按分类筛选 —— 仅返回指定分类的待接单订单</li>
     * </ol>
     */
    @Nested
    @DisplayName("任务大厅")
    class HallTests {

        /**
         * 测试已认证跑腿员查询大厅成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>RunnerAuth 查询返回认证通过记录（authStatus=1, currentFlag=1）</li>
         *   <li>订单查询返回满足条件的订单（orderStatus=WAITING_ACCEPT, payStatus=PAID, runnerId=null）</li>
         *   <li>返回分页结果，分类名称正确填充</li>
         * </ul>
         */
        @Test
        @DisplayName("已认证跑腿员查询大厅成功")
        void shouldReturnHallOrdersForApprovedRunner() {
            // Given: 用户是已认证跑腿员
            Long runnerId = 2L;
            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            // 构建待接单订单
            ErrandOrder waitingOrder = buildOrder(1L, "ER20240105001", 1L, null, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            waitingOrder.setPayStatus(PayStatusEnum.PAID.getCode());
            Page<ErrandOrder> orderPage = new Page<>(1, 10);
            orderPage.setRecords(Collections.singletonList(waitingOrder));
            orderPage.setTotal(1);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(orderPage);

            ErrandCategory category = buildCategory(1L, "快递代取", BigDecimal.valueOf(3.00), null, 1);
            when(errandCategoryMapper.selectById(1L)).thenReturn(category);

            // When: 查询任务大厅
            IPage<OrderVO> result = orderService.hall(runnerId, null, 1, 10);

            // Then: 验证分页结果
            assertNotNull(result, "返回结果不应为null");
            assertEquals(1, result.getTotal(), "总数应为1");
            assertEquals(1, result.getRecords().size(), "记录数应为1");

            OrderVO vo = result.getRecords().get(0);
            assertEquals(Long.valueOf(1L), vo.getId(), "订单ID应一致");
            assertEquals("ER20240105001", vo.getOrderNo(), "订单编号应一致");
            assertEquals("快递代取", vo.getCategoryName(), "分类名称应正确");

            // 验证 RunnerAuth 查询
            verify(runnerAuthMapper).selectOne(any(LambdaQueryWrapper.class));
            // 验证订单分页查询
            verify(errandOrderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        /**
         * 测试未认证用户查询大厅失败
         * <p>
         * 当用户未申请跑腿认证或认证未通过时，应抛出 BusinessException，
         * 错误码为 ORDER_HALL_ACCESS_DENIED(5007)。
         */
        @Test
        @DisplayName("未认证用户查询大厅失败")
        void shouldThrowWhenUserNotApprovedRunner() {
            // Given: 用户未认证（RunnerAuth 查询返回 null）
            Long userId = 3L;
            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.hall(userId, null, 1, 10),
                    "未认证用户查询大厅应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_HALL_ACCESS_DENIED.getCode(), exception.getCode(),
                    "错误码应为 ORDER_HALL_ACCESS_DENIED(5007)");
            assertEquals(ErrorCode.ORDER_HALL_ACCESS_DENIED.getMessage(), exception.getMessage(),
                    "错误信息应为: 任务大厅仅对跑腿员开放");

            // 验证未执行后续订单查询
            verify(errandOrderMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        /**
         * 测试大厅按分类筛选
         * <p>
         * 当传入 categoryId 时，仅返回该分类的待接单订单。
         */
        @Test
        @DisplayName("大厅应按分类筛选")
        void shouldFilterByCategoryId() {
            // Given: 用户是已认证跑腿员
            Long runnerId = 2L;
            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            Long categoryId = 5L;
            ErrandOrder waitingOrder = buildOrder(2L, "ER20240105002", 1L, null, categoryId,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            waitingOrder.setPayStatus(PayStatusEnum.PAID.getCode());

            Page<ErrandOrder> orderPage = new Page<>(1, 10);
            orderPage.setRecords(Collections.singletonList(waitingOrder));
            orderPage.setTotal(1);

            when(errandOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(orderPage);

            ErrandCategory category = buildCategory(categoryId, "文件代取", BigDecimal.valueOf(5.00), null, 1);
            when(errandCategoryMapper.selectById(categoryId)).thenReturn(category);

            // When: 查询指定分类的任务大厅
            IPage<OrderVO> result = orderService.hall(runnerId, categoryId, 1, 10);

            // Then: 验证仅返回指定分类的订单
            assertNotNull(result, "返回结果不应为null");
            assertEquals(1, result.getTotal(), "总数应为1");
            assertEquals(1, result.getRecords().size(), "记录数应为1");
            assertEquals(categoryId, result.getRecords().get(0).getCategoryId(), "分类ID应一致");
            assertEquals("文件代取", result.getRecords().get(0).getCategoryName(), "分类名称应正确");
        }
    }

    // =========================================================================
    // 接单功能测试组
    // =========================================================================

    /**
     * 接单功能测试组
     * <p>
     * 覆盖接单功能的六种核心场景：
     * <ol>
     *   <li>接单成功 —— 更新订单状态并写入状态日志</li>
     *   <li>订单不存在失败</li>
     *   <li>未支付订单不能接</li>
     *   <li>已被接单订单不能重复接</li>
     *   <li>不能接自己发布的订单</li>
     *   <li>接单成功后写入状态日志</li>
     * </ol>
     */
    @Nested
    @DisplayName("接单功能")
    class AcceptOrderTests {

        /**
         * 测试接单成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>RunnerAuth 查询返回认证通过记录</li>
         *   <li>订单查询返回满足接单条件的订单</li>
         *   <li>条件更新成功（runnerId, orderStatus=ACCEPTED, acceptTime）</li>
         *   <li>更新条件包含 pay_status=PAID，防止并发状态下支付状态变化导致的问题</li>
         *   <li>写入状态流转日志（beforeStatus=WAITING_ACCEPT, afterStatus=ACCEPTED, triggerAction=ACCEPT_ORDER, operatorRole=RUNNER）</li>
         * </ul>
         */
        @Test
        @DisplayName("接单成功")
        void shouldAcceptOrderSuccessfully() {
            // Given: 跑腿员已认证，订单满足接单条件
            Long runnerId = 2L;
            Long orderId = 100L;

            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            ErrandOrder order = buildOrder(orderId, "ER20240106001", 1L, null, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: 执行接单
            orderService.accept(orderId, runnerId);

            // Then: 验证订单更新条件包含 pay_status=PAID
            @SuppressWarnings("unchecked")
            ArgumentCaptor<UpdateWrapper<ErrandOrder>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
            verify(errandOrderMapper).update(any(), wrapperCaptor.capture());

            UpdateWrapper<ErrandOrder> capturedWrapper = wrapperCaptor.getValue();
            String sql = capturedWrapper.getSqlSegment();
            assertTrue(sql.contains("pay_status"), "更新条件应包含 pay_status");
            assertTrue(sql.contains("runner_id"), "更新条件应包含 runner_id");
            assertTrue(sql.contains("order_status"), "更新条件应包含 order_status");

            // 验证状态日志写入
            ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            OrderStatusLog statusLog = logCaptor.getValue();

            assertEquals(orderId, statusLog.getOrderId(), "日志关联的订单ID应一致");
            assertEquals("ER20240106001", statusLog.getOrderNo(), "日志中的订单编号应一致");
            assertEquals(OrderStatusEnum.WAITING_ACCEPT.getCode(), statusLog.getBeforeStatus(),
                    "变更前状态应为 WAITING_ACCEPT(1)");
            assertEquals(OrderStatusEnum.ACCEPTED.getCode(), statusLog.getAfterStatus(),
                    "变更后状态应为 ACCEPTED(2)");
            assertEquals("ACCEPT_ORDER", statusLog.getTriggerAction(), "触发动作应为 ACCEPT_ORDER");
            assertEquals(runnerId, statusLog.getOperatorUserId(), "操作人ID应为接单人ID");
            assertEquals("RUNNER", statusLog.getOperatorRole(), "操作人角色应为 RUNNER");
        }

        /**
         * 测试订单不存在失败
         * <p>
         * 当 errandOrderMapper.selectById 返回 null 时，
         * 应抛出 BusinessException，错误码为 ORDER_NOT_FOUND(5001)。
         */
        @Test
        @DisplayName("订单不存在失败")
        void shouldThrowWhenOrderNotFound() {
            // Given: 跑腿员已认证，但订单不存在
            Long runnerId = 2L;
            Long orderId = 999L;

            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));
            when(errandOrderMapper.selectById(orderId)).thenReturn(null);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.accept(orderId, runnerId),
                    "订单不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_FOUND(5001)");
            assertEquals(ErrorCode.ORDER_NOT_FOUND.getMessage(), exception.getMessage(),
                    "错误信息应为: 订单不存在");

            // 验证未执行更新操作
            verify(errandOrderMapper, never()).update(any(), any());
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试未支付订单不能接
         * <p>
         * 当订单 payStatus != PAID 时，应抛出 BusinessException，
         * 错误码为 ORDER_NOT_PAID(5005)。
         */
        @Test
        @DisplayName("未支付订单不能接")
        void shouldThrowWhenOrderNotPaid() {
            // Given: 跑腿员已认证，订单存在但未支付
            Long runnerId = 2L;
            Long orderId = 100L;

            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            ErrandOrder order = buildOrder(orderId, "ER20240106002", 1L, null, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.UNPAID.getCode()); // 未支付
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.accept(orderId, runnerId),
                    "未支付订单应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_NOT_PAID.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_PAID(5005)");
            assertEquals(ErrorCode.ORDER_NOT_PAID.getMessage(), exception.getMessage(),
                    "错误信息应为: 订单未支付");

            // 验证未执行更新操作
            verify(errandOrderMapper, never()).update(any(), any());
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试已被接单订单不能重复接
         * <p>
         * 当订单 runnerId != null 时（已被接单），
         * 应抛出 BusinessException，错误码为 ORDER_ALREADY_ACCEPTED(5006)。
         */
        @Test
        @DisplayName("已被接单订单不能重复接")
        void shouldThrowWhenOrderAlreadyAccepted() {
            // Given: 跑腿员已认证，订单已被其他跑腿员接单
            Long runnerId = 2L;
            Long orderId = 100L;

            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            ErrandOrder order = buildOrder(orderId, "ER20240106003", 1L, 3L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode()); // runnerId=3 已被接单
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.accept(orderId, runnerId),
                    "已被接单订单应抛出 BusinessException");

            assertEquals(ErrorCode.ORDER_ALREADY_ACCEPTED.getCode(), exception.getCode(),
                    "错误码应为 ORDER_ALREADY_ACCEPTED(5006)");
            assertEquals(ErrorCode.ORDER_ALREADY_ACCEPTED.getMessage(), exception.getMessage(),
                    "错误信息应为: 订单已被接单");

            // 验证未执行更新操作
            verify(errandOrderMapper, never()).update(any(), any());
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试不能接自己发布的订单
         * <p>
         * 当订单 publisherId == runnerId 时（自己发布的订单），
         * 应抛出 BusinessException，错误码为 ORDER_CANNOT_ACCEPT_SELF(5011)。
         */
        @Test
        @DisplayName("不能接自己发布的订单")
        void shouldThrowWhenAcceptSelfOrder() {
            // Given: 跑腿员已认证，但尝试接自己发布的订单
            Long runnerId = 2L;
            Long orderId = 100L;

            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            ErrandOrder order = buildOrder(orderId, "ER20240106004", runnerId, null, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode()); // publisherId = runnerId 自己发布
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: 应抛出 BusinessException
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.accept(orderId, runnerId),
                    "不能接自己发布的订单");

            assertEquals(ErrorCode.ORDER_CANNOT_ACCEPT_SELF.getCode(), exception.getCode(),
                    "错误码应为 ORDER_CANNOT_ACCEPT_SELF(5011)");
            assertEquals(ErrorCode.ORDER_CANNOT_ACCEPT_SELF.getMessage(), exception.getMessage(),
                    "错误信息应为: 不能接自己发布的订单");

            // 验证未执行更新操作
            verify(errandOrderMapper, never()).update(any(), any());
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        }

        /**
         * 测试接单成功后写入状态日志
         * <p>
         * 验证 accept() 方法正确写入 order_status_log，
         * beforeStatus=WAITING_ACCEPT(1), afterStatus=ACCEPTED(2),
         * triggerAction="ACCEPT_ORDER", operatorRole="RUNNER"。
         */
        @Test
        @DisplayName("接单成功后写入状态日志")
        void shouldWriteStatusLogWhenAcceptSuccess() {
            // Given: 接单成功的前置条件
            Long runnerId = 2L;
            Long orderId = 100L;

            when(runnerAuthMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(buildRunnerAuth(runnerId, AuthStatusEnum.APPROVED.getCode(), 1));

            ErrandOrder order = buildOrder(orderId, "ER20240106005", 1L, null, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: 执行接单
            orderService.accept(orderId, runnerId);

            // Then: 验证状态日志内容
            ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            OrderStatusLog statusLog = logCaptor.getValue();

            assertEquals(orderId, statusLog.getOrderId(), "订单ID应一致");
            assertEquals("ER20240106005", statusLog.getOrderNo(), "订单编号应一致");
            assertEquals(Integer.valueOf(1), statusLog.getBeforeStatus(),
                    "变更前状态应为 WAITING_ACCEPT(1)");
            assertEquals(Integer.valueOf(2), statusLog.getAfterStatus(),
                    "变更后状态应为 ACCEPTED(2)");
            assertEquals("ACCEPT_ORDER", statusLog.getTriggerAction(), "触发动作应为 ACCEPT_ORDER");
            assertEquals(runnerId, statusLog.getOperatorUserId(), "操作人ID应为接单人");
            assertEquals("RUNNER", statusLog.getOperatorRole(), "操作人角色应为 RUNNER");
        }
    }

    // =========================================================================
    // 订单流程状态测试组
    // =========================================================================

    /**
     * 订单流程状态测试组
     * <p>
     * 覆盖 contact/pickup/deliver/complete 四种状态流转：
     * <ol>
     *   <li>contact: ACCEPTED(2) → CONTACTED(3)，操作者 RUNNER</li>
     *   <li>pickup: CONTACTED(3) → PICKED_UP(4)，操作者 RUNNER</li>
     *   <li>deliver: PICKED_UP(4) → DELIVERED(6)，操作者 RUNNER</li>
     *   <li>complete: DELIVERED(6) → COMPLETED(7)，操作者 STUDENT</li>
     * </ol>
     * 每个流转包含：成功场景、权限错误场景、状态错误场景、并发冲突场景。
     */
    @Nested
    @DisplayName("订单流程状态")
    class OrderFlowTests {

        // ==================== contact 测试 ====================

        /**
         * 测试 contact 成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单状态为 ACCEPTED(2)，发布人ID为1L</li>
         *   <li>runnerId=2L 执行 contact，条件更新包含 orderStatus 和 runnerId</li>
         *   <li>状态流转日志记录 beforeStatus=ACCEPTED(2), afterStatus=CONTACTED(3)</li>
         *   <li>日志 triggerAction="CONTACT_ORDER"，operatorRole="RUNNER"</li>
         * </ul>
         */
        @Test
        @DisplayName("contact 成功")
        void shouldContactOrderSuccessfully() {
            // Given
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107001", 1L, runnerId, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When
            orderService.contact(orderId, runnerId);

            // Then: 验证条件更新包含 orderStatus 和 runnerId
            verify(errandOrderMapper).update(any(), updateWrapperCaptor.capture());
            String sql = updateWrapperCaptor.getValue().getSqlSegment();
            assertTrue(sql.contains("order_status"), "更新条件应包含 order_status");
            assertTrue(sql.contains("runner_id"), "更新条件应包含 runner_id");

            // 验证日志
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            assertEquals(OrderStatusEnum.CONTACTED.getCode(), logCaptor.getValue().getAfterStatus(),
                    "变更后状态应为 CONTACTED(3)");
            assertEquals("CONTACT_ORDER", logCaptor.getValue().getTriggerAction(),
                    "触发动作应为 CONTACT_ORDER");
        }

        /**
         * 测试 contact 时用户不是跑腿员（runnerId 不匹配 publisherId）
         * <p>
         * 当 runnerId=2L 但 order.publisherId=1L 时，抛出 ORDER_NOT_OWNED。
         */
        @Test
        @DisplayName("contact 时用户不是跑腿员应抛出 ORDER_NOT_OWNED")
        void shouldThrowWhenContactNotRunner() {
            // Given: runnerId=2L, order.runnerId=2L 但 order.publisherId=1L
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107002", 1L, runnerId, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            // runnerId=2L 是接单人，但尝试联系时用户ID不匹配（实际应该是 runnerId 但联系人是发布人？）
            // 根据需求描述：runnerId 不匹配 publisherId，运行者不是跑腿员，抛出 ORDER_NOT_OWNED
            // 这里 runnerId=2L, publisherId=1L，说明 runnerId 不是发布人，应为 RUNNER 角色
            // 权限检查逻辑：order.runnerId != runnerId
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: runnerId=99L 不匹配 order.runnerId=2L，显式校验抛出 ORDER_NOT_OWNED
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.contact(orderId, 99L),
                    "runnerId 不匹配时应抛出 ORDER_NOT_OWNED");

            assertEquals(ErrorCode.ORDER_NOT_OWNED.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_OWNED(5002)");
        }

        /**
         * 测试 contact 时订单状态不正确
         * <p>
         * 当订单状态不是 ACCEPTED 时，抛出 ORDER_STATUS_CONFLICT。
         */
        @Test
        @DisplayName("contact 时状态不是 ACCEPTED 应抛出 ORDER_STATUS_CONFLICT")
        void shouldThrowWhenContactWrongStatus() {
            // Given: 订单状态为 WAITING_ACCEPT(1)，不是 ACCEPTED(2)
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107003", 1L, runnerId, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.contact(orderId, runnerId),
                    "状态不是 ACCEPTED 时应抛出 ORDER_STATUS_CONFLICT");

            assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode(),
                    "错误码应为 ORDER_STATUS_CONFLICT(5003)");
        }

        // ==================== pickup 测试 ====================

        /**
         * 测试 pickup 成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单状态为 CONTACTED(3)，runnerId=2L</li>
         *   <li>runnerId=2L 执行 pickup，状态更新为 PICKED_UP(4)</li>
         *   <li>状态流转日志 triggerAction="PICKUP_ORDER"，operatorRole="RUNNER"</li>
         * </ul>
         */
        @Test
        @DisplayName("pickup 成功")
        void shouldPickupOrderSuccessfully() {
            // Given
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107004", 1L, runnerId, 1L,
                    OrderStatusEnum.CONTACTED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When
            orderService.pickup(orderId, runnerId);

            // Then: 验证更新
            verify(errandOrderMapper).update(any(), updateWrapperCaptor.capture());
            assertTrue(updateWrapperCaptor.getValue().getSqlSegment().contains("order_status"),
                    "更新条件应包含 order_status");

            // 验证日志
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            assertEquals(OrderStatusEnum.PICKED_UP.getCode(), logCaptor.getValue().getAfterStatus(),
                    "变更后状态应为 PICKED_UP(4)");
            assertEquals("PICKUP_ORDER", logCaptor.getValue().getTriggerAction(),
                    "触发动作应为 PICKUP_ORDER");
            assertEquals("RUNNER", logCaptor.getValue().getOperatorRole(),
                    "操作人角色应为 RUNNER");
        }

        /**
         * 测试 pickup 时订单状态不是 CONTACTED
         * <p>
         * 当订单状态不是 CONTACTED 时，抛出 ORDER_STATUS_CONFLICT。
         */
        @Test
        @DisplayName("pickup 时状态不是 CONTACTED 应抛出 ORDER_STATUS_CONFLICT")
        void shouldThrowWhenPickupWrongStatus() {
            // Given: 订单状态为 ACCEPTED(2)，不是 CONTACTED(3)
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107005", 1L, runnerId, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.pickup(orderId, runnerId),
                    "状态不是 CONTACTED 时应抛出 ORDER_STATUS_CONFLICT");

            assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode(),
                    "错误码应为 ORDER_STATUS_CONFLICT(5003)");
        }

        // ==================== deliver 测试 ====================

        /**
         * 测试 deliver 成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单状态为 PICKED_UP(4)，runnerId=2L</li>
         *   <li>runnerId=2L 执行 deliver，状态更新为 DELIVERED(6)</li>
         *   <li>状态流转日志 triggerAction="DELIVER_ORDER"，operatorRole="RUNNER"</li>
         * </ul>
         */
        @Test
        @DisplayName("deliver 成功")
        void shouldDeliverOrderSuccessfully() {
            // Given
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107006", 1L, runnerId, 1L,
                    OrderStatusEnum.PICKED_UP.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When
            orderService.deliver(orderId, runnerId);

            // Then: 验证更新
            verify(errandOrderMapper).update(any(), updateWrapperCaptor.capture());
            assertTrue(updateWrapperCaptor.getValue().getSqlSegment().contains("order_status"),
                    "更新条件应包含 order_status");

            // 验证日志
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            assertEquals(OrderStatusEnum.DELIVERED.getCode(), logCaptor.getValue().getAfterStatus(),
                    "变更后状态应为 DELIVERED(6)");
            assertEquals("DELIVER_ORDER", logCaptor.getValue().getTriggerAction(),
                    "触发动作应为 DELIVER_ORDER");
            assertEquals("RUNNER", logCaptor.getValue().getOperatorRole(),
                    "操作人角色应为 RUNNER");
        }

        /**
         * 测试 deliver 时订单状态不是 PICKED_UP
         * <p>
         * 当订单状态不是 PICKED_UP 时，抛出 ORDER_STATUS_CONFLICT。
         */
        @Test
        @DisplayName("deliver 时状态不是 PICKED_UP 应抛出 ORDER_STATUS_CONFLICT")
        void shouldThrowWhenDeliverWrongStatus() {
            // Given: 订单状态为 CONTACTED(3)，不是 PICKED_UP(4)
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107007", 1L, runnerId, 1L,
                    OrderStatusEnum.CONTACTED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.deliver(orderId, runnerId),
                    "状态不是 PICKED_UP 时应抛出 ORDER_STATUS_CONFLICT");

            assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode(),
                    "错误码应为 ORDER_STATUS_CONFLICT(5003)");
        }

        // ==================== complete 测试 ====================

        /**
         * 测试 complete 成功
         * <p>
         * 验证点：
         * <ul>
         *   <li>订单状态为 DELIVERED(6)，publisherId=1L</li>
         *   <li>userId=1L（发布人）执行 complete，状态更新为 COMPLETED(7)</li>
         *   <li>状态流转日志 triggerAction="COMPLETE_ORDER"，operatorRole="STUDENT"</li>
         * </ul>
         */
        @Test
        @DisplayName("complete 成功")
        void shouldCompleteOrderSuccessfully() {
            // Given
            Long publisherId = 1L;
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107008", publisherId, runnerId, 1L,
                    OrderStatusEnum.DELIVERED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            // When: userId=1L 是发布人
            orderService.complete(orderId, publisherId);

            // Then: 验证更新
            verify(errandOrderMapper).update(any(), updateWrapperCaptor.capture());
            assertTrue(updateWrapperCaptor.getValue().getSqlSegment().contains("order_status"),
                    "更新条件应包含 order_status");

            // 验证日志
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            assertEquals(OrderStatusEnum.COMPLETED.getCode(), logCaptor.getValue().getAfterStatus(),
                    "变更后状态应为 COMPLETED(7)");
            assertEquals("COMPLETE_ORDER", logCaptor.getValue().getTriggerAction(),
                    "触发动作应为 COMPLETE_ORDER");
            assertEquals("STUDENT", logCaptor.getValue().getOperatorRole(),
                    "操作人角色应为 STUDENT");
        }

        /**
         * 测试 complete 时用户不是发布人
         * <p>
         * 当 userId=2L（跑腿员）尝试 complete，但发布人是 1L 时，抛出 ORDER_NOT_OWNED。
         */
        @Test
        @DisplayName("complete 时用户不是发布人应抛出 ORDER_NOT_OWNED")
        void shouldThrowWhenCompleteNotPublisher() {
            // Given: publisherId=1L, runnerId=2L，userId=2L 尝试 complete
            Long publisherId = 1L;
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107009", publisherId, runnerId, 1L,
                    OrderStatusEnum.DELIVERED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then: userId=2L 不匹配 publisherId=1L，显式校验抛出 ORDER_NOT_OWNED
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.complete(orderId, runnerId),
                    "userId 不匹配 publisherId 时应抛出 ORDER_NOT_OWNED");

            assertEquals(ErrorCode.ORDER_NOT_OWNED.getCode(), exception.getCode(),
                    "错误码应为 ORDER_NOT_OWNED(5002)");
        }

        /**
         * 测试 complete 时订单状态不是 DELIVERED
         * <p>
         * 当订单状态不是 DELIVERED 时，抛出 ORDER_STATUS_CONFLICT。
         */
        @Test
        @DisplayName("complete 时状态不是 DELIVERED 应抛出 ORDER_STATUS_CONFLICT")
        void shouldThrowWhenCompleteWrongStatus() {
            // Given: 订单状态为 PICKED_UP(4)，不是 DELIVERED(6)
            Long publisherId = 1L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107010", publisherId, 2L, 1L,
                    OrderStatusEnum.PICKED_UP.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.complete(orderId, publisherId),
                    "状态不是 DELIVERED 时应抛出 ORDER_STATUS_CONFLICT");

            assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode(),
                    "错误码应为 ORDER_STATUS_CONFLICT(5003)");
        }

        // ==================== 条件更新失败测试 ====================

        /**
         * 测试条件更新返回 0 时抛出异常
         * <p>
         * 当 errandOrderMapper.update 返回 0 行时，说明并发冲突或条件不匹配，
         * 应抛出 ORDER_STATUS_CONFLICT。
         */
        @Test
        @DisplayName("条件更新返回 0 时应抛出 ORDER_STATUS_CONFLICT")
        void shouldThrowWhenUpdateReturnsZero() {
            // Given: contact 场景，但 update 返回 0
            Long runnerId = 2L;
            Long orderId = 100L;

            ErrandOrder order = buildOrder(orderId, "ER20240107011", 1L, runnerId, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(0).when(errandOrderMapper).update(any(), any()); // 更新失败

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.contact(orderId, runnerId),
                    "update 返回 0 时应抛出 ORDER_STATUS_CONFLICT");

            assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode(),
                    "错误码应为 ORDER_STATUS_CONFLICT(5003)");
        }
    }

    // =========================================================================
    // 取消订单测试组
    // =========================================================================

    /**
     * 取消订单功能测试组
     * <p>
     * 覆盖取消订单的十种核心场景：
     * <ol>
     *   <li>发布人取消未支付订单成功 —— 无需创建退款记录</li>
     *   <li>发布人取消已支付订单成功 —— 创建退款记录</li>
     *   <li>跑腿员取消已接单订单成功 —— 也需创建退款记录</li>
     *   <li>非订单所属人取消失败 —— 抛出 ORDER_NOT_OWNED</li>
     *   <li>已完成订单不能取消 —— 抛出 ORDER_CANNOT_CANCEL</li>
     *   <li>已取消订单不能重复取消</li>
     *   <li>条件更新返回0时抛状态冲突异常</li>
     *   <li>取消成功写入状态日志</li>
     *   <li>未支付订单取消不创建退款记录</li>
     *   <li>已送达订单不允许取消</li>
     * </ol>
     */
    @Nested
    @DisplayName("取消订单")
    class CancelOrderTests {

        /**
         * 测试发布人取消未支付订单成功
         * <p>
         * 当订单 payStatus=UNPAID 时取消，不应创建退款记录。
         * 仅更新订单状态为 CANCELLED 并写入状态流转日志。
         */
        @Test
        @DisplayName("发布人取消未支付订单成功")
        void shouldCancelUnpaidOrderAsPublisher() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("不需要了");

            ErrandOrder order = buildOrder(orderId, "ER20240108001", publisherId, 2L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.UNPAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            orderService.cancel(orderId, publisherId, request);

            verify(errandOrderMapper).update(any(), any());
            verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
            // 未支付订单不应创建退款记录
            verify(refundRecordMapper, never()).insert(any(RefundRecord.class));
        }

        /**
         * 测试发布人取消已支付订单成功并创建退款记录
         * <p>
         * 当订单 payStatus=PAID 时取消，应创建退款记录（RefundRecord），
         * 退款状态为 PENDING，退款原因使用请求中的 cancelReason。
         */
        @Test
        @DisplayName("发布人取消已支付订单成功并创建退款记录")
        void shouldCancelPaidOrderAndCreateRefundRecord() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("临时有事");

            ErrandOrder order = buildOrder(orderId, "ER20240108002", publisherId, 2L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            order.setOrderAmount(BigDecimal.valueOf(10.00));
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);
            // 支付单查询
            PaymentOrder payOrder = new PaymentOrder();
            payOrder.setOrderId(orderId);
            payOrder.setPayNo("PAY123456789");
            when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payOrder);

            orderService.cancel(orderId, publisherId, request);

            verify(refundRecordMapper).insert(refundRecordCaptor.capture());
            RefundRecord refund = refundRecordCaptor.getValue();
            assertEquals(orderId, refund.getOrderId());
            assertNotNull(refund.getRequestId(), "requestId 不应为空");
            assertEquals(request.getCancelReason(), refund.getRefundReason());
            assertEquals(Integer.valueOf(RefundStatusEnum.PENDING.getCode()), refund.getRefundStatus());
            assertEquals("PAY123456789", refund.getPayNo(), "退款记录应关联支付单号");
        }

        /**
         * 测试跑腿员取消已接单订单成功
         * <p>
         * 当 runnerId 取消订单时（runnerId=order.runnerId），也应创建退款记录。
         * 无论取消操作由发布人还是跑腿员发起，已支付订单的退款逻辑一致。
         */
        @Test
        @DisplayName("跑腿员取消已接单订单成功")
        void shouldCancelAsRunner() {
            Long runnerId = 2L;
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("太远了");

            ErrandOrder order = buildOrder(orderId, "ER20240108003", publisherId, runnerId, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);

            orderService.cancel(orderId, runnerId, request);

            verify(errandOrderMapper).update(any(), any());
            verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
            // 跑腿员取消也应创建退款记录
            verify(refundRecordMapper).insert(any(RefundRecord.class));
        }

        /**
         * 测试非订单所属人取消失败
         * <p>
         * 当 userId 既不是 publisherId 也不是 runnerId 时，
         * 应抛出 BusinessException，错误码为 ORDER_NOT_OWNED(5002)。
         */
        @Test
        @DisplayName("非订单所属人取消失败")
        void shouldThrowWhenCancelNotOwner() {
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("test");

            ErrandOrder order = buildOrder(orderId, "ER20240108004", 1L, 2L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.cancel(orderId, 99L, request));
            assertEquals(ErrorCode.ORDER_NOT_OWNED.getCode(), exception.getCode());
        }

        /**
         * 测试已完成订单不能取消
         * <p>
         * 当订单状态为 COMPLETED 时，应抛出 BusinessException，
         * 错误码为 ORDER_CANNOT_CANCEL(5008)。
         */
        @Test
        @DisplayName("已完成订单不能取消")
        void shouldThrowWhenCancelCompletedOrder() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("test");

            ErrandOrder order = buildOrder(orderId, "ER20240108005", publisherId, 2L, 1L,
                    OrderStatusEnum.COMPLETED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.cancel(orderId, publisherId, request));
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL.getCode(), exception.getCode());
        }

        /**
         * 测试已取消订单不能重复取消
         * <p>
         * 当订单状态为 CANCELLED 时，应抛出 BusinessException，
         * 错误码为 ORDER_CANNOT_CANCEL(5008)。
         */
        @Test
        @DisplayName("已取消订单不能重复取消")
        void shouldThrowWhenCancelAlreadyCancelled() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("test");

            ErrandOrder order = buildOrder(orderId, "ER20240108006", publisherId, 2L, 1L,
                    OrderStatusEnum.CANCELLED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.cancel(orderId, publisherId, request));
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL.getCode(), exception.getCode());
        }

        /**
         * 测试条件更新返回0时抛状态冲突异常
         * <p>
         * 当 errandOrderMapper.update 返回 0 行时，说明并发冲突或订单状态已变化，
         * 应抛出 BusinessException，错误码为 ORDER_STATUS_CONFLICT(5003)。
         * 同时不应写入状态日志或退款记录。
         */
        @Test
        @DisplayName("条件更新返回0时抛状态冲突异常")
        void shouldThrowWhenUpdateReturnsZero() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("test");

            ErrandOrder order = buildOrder(orderId, "ER20240108007", publisherId, 2L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(0).when(errandOrderMapper).update(any(), any());

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.cancel(orderId, publisherId, request));
            assertEquals(ErrorCode.ORDER_STATUS_CONFLICT.getCode(), exception.getCode());
            verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
            verify(refundRecordMapper, never()).insert(any(RefundRecord.class));
        }

        /**
         * 测试取消成功写入状态日志
         * <p>
         * 验证 cancel() 方法正确写入 order_status_log，
         * beforeStatus=WAITING_ACCEPT(1), afterStatus=CANCELLED(8),
         * triggerAction="CANCEL_ORDER"。
         */
        @Test
        @DisplayName("取消成功写入状态日志")
        void shouldWriteStatusLogOnCancel() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("不需要了");

            ErrandOrder order = buildOrder(orderId, "ER20240108008", publisherId, 2L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.UNPAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            orderService.cancel(orderId, publisherId, request);

            verify(orderStatusLogMapper).insert(logCaptor.capture());
            OrderStatusLog log = logCaptor.getValue();
            assertEquals(orderId, log.getOrderId());
            assertEquals(OrderStatusEnum.WAITING_ACCEPT.getCode(), log.getBeforeStatus());
            assertEquals(OrderStatusEnum.CANCELLED.getCode(), log.getAfterStatus());
            assertEquals("CANCEL_ORDER", log.getTriggerAction());
        }

        /**
         * 测试未支付订单取消不创建退款记录
         * <p>
         * 当订单 payStatus=UNPAID 时，即使状态为 ACCEPTED 取消也不应创建退款记录。
         * 仅更新订单状态并写入日志。
         */
        @Test
        @DisplayName("未支付订单取消不创建退款记录")
        void shouldNotCreateRefundForUnpaidOrder() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("不需要了");

            ErrandOrder order = buildOrder(orderId, "ER20240108009", publisherId, 2L, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            order.setPayStatus(PayStatusEnum.UNPAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            orderService.cancel(orderId, publisherId, request);

            verify(refundRecordMapper, never()).insert(any(RefundRecord.class));
        }

        /**
         * 测试已送达订单不允许取消
         * <p>
         * 当订单状态为 DELIVERED(6) 时，应抛出 BusinessException，
         * 错误码为 ORDER_CANNOT_CANCEL(5008)。
         */
        @Test
        @DisplayName("已送达订单不允许取消")
        void shouldThrowWhenCancelDeliveredOrder() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("test");

            ErrandOrder order = buildOrder(orderId, "ER20240108010", publisherId, 2L, 1L,
                    OrderStatusEnum.DELIVERED.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.cancel(orderId, publisherId, request));
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL.getCode(), exception.getCode());
        }

        /**
         * 发布者取消时应写入 cancelUserId 和 cancelRole=STUDENT
         */
        @Test
        @DisplayName("发布者取消时应写入 cancelUserId 和 cancelRole=STUDENT")
        void shouldSetCancelAuditForPublisher() {
            Long publisherId = 1L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("不需要了");

            ErrandOrder order = buildOrder(orderId, "ER_CANCEL_001", publisherId, 2L, 1L,
                    OrderStatusEnum.WAITING_ACCEPT.getCode());
            order.setPayStatus(PayStatusEnum.UNPAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);

            orderService.cancel(orderId, publisherId, request);

            // SET 字段在 getSqlSet() 中，不在 getSqlSegment()（WHERE 部分）
            verify(errandOrderMapper).update(any(), updateWrapperCaptor.capture());
            String sqlSet = updateWrapperCaptor.getValue().getSqlSet();
            assertTrue(sqlSet.contains("cancel_user_id"), "SET 应包含 cancel_user_id");
            assertTrue(sqlSet.contains("cancel_role"), "SET 应包含 cancel_role");
        }

        /**
         * 跑腿员取消时应写入 cancelUserId 和 cancelRole=RUNNER
         */
        @Test
        @DisplayName("跑腿员取消时应写入 cancelUserId 和 cancelRole=RUNNER")
        void shouldSetCancelAuditForRunner() {
            Long runnerId = 2L;
            Long orderId = 100L;
            OrderCancelRequest request = new OrderCancelRequest();
            request.setCancelReason("太远了");

            ErrandOrder order = buildOrder(orderId, "ER_CANCEL_002", 1L, runnerId, 1L,
                    OrderStatusEnum.ACCEPTED.getCode());
            order.setPayStatus(PayStatusEnum.PAID.getCode());
            when(errandOrderMapper.selectById(orderId)).thenReturn(order);
            doReturn(1).when(errandOrderMapper).update(any(), any());
            when(orderStatusLogMapper.insert(any(OrderStatusLog.class))).thenReturn(1);
            when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);

            orderService.cancel(orderId, runnerId, request);

            verify(errandOrderMapper).update(any(), updateWrapperCaptor.capture());
            String sqlSet = updateWrapperCaptor.getValue().getSqlSet();
            assertTrue(sqlSet.contains("cancel_user_id"), "SET 应包含 cancel_user_id");
            assertTrue(sqlSet.contains("cancel_role"), "SET 应包含 cancel_role");
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 构建测试用分类对象
     *
     * @param id              分类ID
     * @param categoryName    分类名称
     * @param baseFee         基础费用
     * @param distanceFeeRule 距离收费规则JSON
     * @param categoryStatus  分类状态（1启用/2停用）
     * @return 分类实体
     */
    private ErrandCategory buildCategory(Long id, String categoryName, BigDecimal baseFee,
                                          String distanceFeeRule, Integer categoryStatus) {
        ErrandCategory category = new ErrandCategory();
        category.setId(id);
        category.setCategoryName(categoryName);
        category.setCategoryCode("CAT_" + id);
        category.setBaseFee(baseFee);
        category.setDistanceFeeRule(distanceFeeRule);
        category.setCategoryStatus(categoryStatus);
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
     * 构建测试用订单状态流转日志对象
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

    /**
     * 构建测试用跑腿认证对象
     *
     * @param userId       用户ID
     * @param authStatus   认证状态（1通过/0待审/2驳回/3失效）
     * @param currentFlag  当前标识（1当前提交记录/0历史记录）
     * @return 跑腿认证实体
     */
    private RunnerAuth buildRunnerAuth(Long userId, Integer authStatus, Integer currentFlag) {
        RunnerAuth auth = new RunnerAuth();
        auth.setId(1L);
        auth.setUserId(userId);
        auth.setAuthStatus(authStatus);
        auth.setCurrentFlag(currentFlag);
        auth.setAuthBatchNo("AUTH_" + System.currentTimeMillis());
        auth.setStudentNo("20240001");
        auth.setSchoolName("测试学校");
        auth.setCampusName("测试校区");
        auth.setCertType(1);
        auth.setCertNo("123456789012345678");
        return auth;
    }
}
