package com.example.backend.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付宝沙箱支付服务
 */
public interface AlipayService {

    /**
     * 生成电脑网站支付表单 HTML
     * @param orderNo 订单编号
     * @param amount 支付金额（元）
     * @param subject 商品标题
     * @return 支付宝支付页面 HTML 表单，前端直接输出到页面即可跳转
     */
    String buildPayForm(Long orderId, String orderNo, String amount, String subject);

    /**
     * 验证支付宝异步通知签名
     * @return true 验签通过
     */
    boolean verifyNotifySign(HttpServletRequest request);

    /**
     * 验证支付是否成功（解析通知参数中的 trade_status）
     */
    boolean isTradeSuccess(HttpServletRequest request);
}
