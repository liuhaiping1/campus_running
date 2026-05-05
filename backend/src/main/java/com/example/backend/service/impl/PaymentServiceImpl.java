package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.entity.PaymentOrder;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.service.AlipayService;
import com.example.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final ErrandOrderMapper errandOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final AlipayService alipayService;

    private static final String PAY_ORDER_ACTION = "PAY_ORDER";
    private static final String OPERATOR_ROLE_STUDENT = "STUDENT";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String pay(Long orderId, Long userId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
        if (PayStatusEnum.PAID.getCode().equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        LocalDateTime now = LocalDateTime.now();
        if (PayStatusEnum.PAYING.getCode().equals(order.getPayStatus())) {
            PaymentOrder existingPayOrder = selectLatestPaymentOrder(order.getOrderNo());
            if (existingPayOrder != null
                    && existingPayOrder.getExpireTime() != null
                    && existingPayOrder.getExpireTime().isAfter(now)) {
                String amount = order.getOrderAmount().setScale(2, RoundingMode.HALF_UP).toString();
                return alipayService.buildPayForm(order.getId(), order.getOrderNo(), amount, order.getTitle());
            }
        } else if (!PayStatusEnum.UNPAID.getCode().equals(order.getPayStatus())
                && !PayStatusEnum.PAYMENT_CLOSED.getCode().equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "当前支付状态不允许发起支付");
        }

        // payment_order.order_id 有唯一约束，重复发起支付时刷新同一条支付记录
        PaymentOrder paymentOrder = selectLatestPaymentOrder(order.getOrderNo());
        if (paymentOrder == null) {
            paymentOrder = new PaymentOrder();
            paymentOrder.setOrderId(order.getId());
            paymentOrder.setOrderNo(order.getOrderNo());
            paymentOrder.setCreateTime(now);
            fillPaymentOrderForPaying(paymentOrder, order, now);
            paymentOrderMapper.insert(paymentOrder);
        } else {
            fillPaymentOrderForPaying(paymentOrder, order, now);
            paymentOrderMapper.updateById(paymentOrder);
        }

        // 订单 → 支付中
        order.setPayStatus(PayStatusEnum.PAYING.getCode());
        order.setUpdateTime(now);
        errandOrderMapper.updateById(order);

        String amount = order.getOrderAmount().setScale(2, RoundingMode.HALF_UP).toString();
        return alipayService.buildPayForm(order.getId(), order.getOrderNo(), amount, order.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(String orderNo, String tradeNo, String totalAmount) {
        ErrandOrder order = errandOrderMapper.selectOne(
                new LambdaQueryWrapper<ErrandOrder>()
                        .eq(ErrandOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            log.error("支付回调订单不存在: orderNo={}", orderNo);
            return "failure";
        }

        // 防止重复通知
        if (PayStatusEnum.PAID.getCode().equals(order.getPayStatus())) {
            log.info("订单 {} 已支付，忽略重复通知", orderNo);
            return "success";
        }

        // 校验支付金额
        String expectedAmount = order.getOrderAmount().setScale(2, RoundingMode.HALF_UP).toString();
        if (!expectedAmount.equals(totalAmount)) {
            log.error("支付金额不匹配: orderNo={}, expected={}, actual={}", orderNo, expectedAmount, totalAmount);
            return "failure";
        }

        LocalDateTime now = LocalDateTime.now();

        // 更新订单状态：支付成功 → 待接单
        order.setPayStatus(PayStatusEnum.PAID.getCode());
        order.setOrderStatus(OrderStatusEnum.WAITING_ACCEPT.getCode());
        order.setUpdateTime(now);
        errandOrderMapper.updateById(order);

        // 更新支付记录
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderNo, orderNo)
                        .orderByDesc(PaymentOrder::getCreateTime)
                        .last("LIMIT 1")
        );
        if (paymentOrder != null) {
            paymentOrder.setPayStatus(PayStatusEnum.PAID.getCode());
            paymentOrder.setTradeNo(tradeNo);
            paymentOrder.setPayTime(now);
            paymentOrder.setUpdateTime(now);
            paymentOrderMapper.updateById(paymentOrder);
        }

        // 写入状态日志
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setBeforeStatus(OrderStatusEnum.UNPAID.getCode());
        statusLog.setAfterStatus(OrderStatusEnum.WAITING_ACCEPT.getCode());
        statusLog.setTriggerAction(PAY_ORDER_ACTION);
        statusLog.setOperatorUserId(order.getPublisherId());
        statusLog.setOperatorRole(OPERATOR_ROLE_STUDENT);
        statusLog.setCreateTime(now);
        statusLog.setUpdateTime(now);
        orderStatusLogMapper.insert(statusLog);

        log.info("支付回调处理完成: orderNo={}, tradeNo={}", orderNo, tradeNo);
        return "success";
    }

    /**
     * 查询订单最近一次支付记录。
     *
     * @param orderNo 订单编号
     * @return 最近一次支付记录，不存在时返回 null
     */
    private PaymentOrder selectLatestPaymentOrder(String orderNo) {
        return paymentOrderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderNo, orderNo)
                        .orderByDesc(PaymentOrder::getCreateTime)
                        .last("LIMIT 1")
        );
    }

    /**
     * 填充支付中状态的支付记录。
     *
     * @param paymentOrder 支付记录
     * @param order        订单
     * @param now          当前时间
     */
    private void fillPaymentOrderForPaying(PaymentOrder paymentOrder, ErrandOrder order, LocalDateTime now) {
        paymentOrder.setPayNo("PAY" + IdWorker.getIdStr());
        paymentOrder.setPayChannel("ALIPAY_SANDBOX");
        paymentOrder.setPayAmount(order.getOrderAmount());
        paymentOrder.setPayStatus(PayStatusEnum.PAYING.getCode());
        paymentOrder.setCallbackStatus(0);
        paymentOrder.setRefundAmount(BigDecimal.ZERO);
        paymentOrder.setExpireTime(now.plusMinutes(30));
        paymentOrder.setUpdateTime(now);
    }
}
