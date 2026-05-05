package com.example.backend.service;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 发起支付：校验订单、创建支付记录、生成支付宝表单
     * @return 支付宝 pagePay 表单 HTML
     */
    String pay(Long orderId, Long userId);

    /**
     * 处理支付宝异步通知：验签、更新订单与支付记录
     * @return "success" 或 "failure"
     */
    String handleNotify(String orderNo, String tradeNo, String totalAmount);
}
