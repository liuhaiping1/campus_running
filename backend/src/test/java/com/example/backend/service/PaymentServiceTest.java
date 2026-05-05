package com.example.backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.PaymentOrder;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.service.impl.PaymentServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 支付服务测试")
class PaymentServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String ORDER_NO = "ER001";

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ErrandOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PaymentOrder.class);
    }

    @Mock
    private ErrandOrderMapper errandOrderMapper;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;

    @Mock
    private AlipayService alipayService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("支付中且未过期时应复用原支付表单")
    void shouldReusePaymentForm_whenPayingOrderNotExpired() {
        ErrandOrder order = buildOrder(PayStatusEnum.PAYING.getCode());
        PaymentOrder paymentOrder = buildPaymentOrder(PayStatusEnum.PAYING.getCode(), LocalDateTime.now().plusMinutes(10));
        when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentOrder);
        when(alipayService.buildPayForm(eq(ORDER_ID), eq(ORDER_NO), eq("5.00"), eq("测试订单")))
                .thenReturn("<form>pay</form>");

        String result = paymentService.pay(ORDER_ID, USER_ID);

        assertEquals("<form>pay</form>", result);
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
        verify(errandOrderMapper, never()).updateById(any(ErrandOrder.class));
    }

    @Test
    @DisplayName("支付中但已过期时应刷新原支付单")
    void shouldRefreshExistingPayment_whenPayingOrderExpired() {
        ErrandOrder order = buildOrder(PayStatusEnum.PAYING.getCode());
        PaymentOrder expiredPayment = buildPaymentOrder(PayStatusEnum.PAYING.getCode(), LocalDateTime.now().minusMinutes(1));
        when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expiredPayment);
        when(alipayService.buildPayForm(eq(ORDER_ID), eq(ORDER_NO), eq("5.00"), eq("测试订单")))
                .thenReturn("<form>new-pay</form>");

        String result = paymentService.pay(ORDER_ID, USER_ID);

        assertEquals("<form>new-pay</form>", result);
        assertEquals(PayStatusEnum.PAYING.getCode(), expiredPayment.getPayStatus());
        assertEquals("ALIPAY_SANDBOX", expiredPayment.getPayChannel());
        verify(paymentOrderMapper).updateById(expiredPayment);
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
        verify(errandOrderMapper).updateById(order);
        assertEquals(PayStatusEnum.PAYING.getCode(), order.getPayStatus());
    }

    @Test
    @DisplayName("已支付订单再次支付时应提示订单已支付")
    void shouldThrowAlreadyPaid_whenOrderPaid() {
        ErrandOrder order = buildOrder(PayStatusEnum.PAID.getCode());
        when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> paymentService.pay(ORDER_ID, USER_ID));

        assertEquals(ErrorCode.ORDER_ALREADY_PAID.getCode(), exception.getCode());
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("未支付订单应创建支付单")
    void shouldCreatePaymentOrder_whenOrderUnpaid() {
        ErrandOrder order = buildOrder(PayStatusEnum.UNPAID.getCode());
        when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(alipayService.buildPayForm(eq(ORDER_ID), eq(ORDER_NO), eq("5.00"), eq("测试订单")))
                .thenReturn("<form>pay</form>");

        String result = paymentService.pay(ORDER_ID, USER_ID);

        assertEquals("<form>pay</form>", result);
        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderMapper).insert(captor.capture());
        assertEquals(PayStatusEnum.PAYING.getCode(), captor.getValue().getPayStatus());
        assertEquals(PayStatusEnum.PAYING.getCode(), order.getPayStatus());
    }

    @Test
    @DisplayName("未支付但已有支付记录时应刷新原支付单")
    void shouldRefreshExistingPaymentOrder_whenUnpaidOrderHasPaymentRecord() {
        ErrandOrder order = buildOrder(PayStatusEnum.UNPAID.getCode());
        PaymentOrder existingPayment = buildPaymentOrder(PayStatusEnum.PAYMENT_CLOSED.getCode(), LocalDateTime.now().minusMinutes(1));
        when(errandOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(paymentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingPayment);
        when(alipayService.buildPayForm(eq(ORDER_ID), eq(ORDER_NO), eq("5.00"), eq("测试订单")))
                .thenReturn("<form>pay</form>");

        String result = paymentService.pay(ORDER_ID, USER_ID);

        assertEquals("<form>pay</form>", result);
        assertEquals(PayStatusEnum.PAYING.getCode(), existingPayment.getPayStatus());
        verify(paymentOrderMapper).updateById(existingPayment);
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    private ErrandOrder buildOrder(Integer payStatus) {
        ErrandOrder order = new ErrandOrder();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setPublisherId(USER_ID);
        order.setTitle("测试订单");
        order.setOrderAmount(new BigDecimal("5.00"));
        order.setPayStatus(payStatus);
        return order;
    }

    private PaymentOrder buildPaymentOrder(Integer payStatus, LocalDateTime expireTime) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(100L);
        paymentOrder.setOrderId(ORDER_ID);
        paymentOrder.setOrderNo(ORDER_NO);
        paymentOrder.setPayStatus(payStatus);
        paymentOrder.setExpireTime(expireTime);
        paymentOrder.setCreateTime(LocalDateTime.now());
        return paymentOrder;
    }
}
