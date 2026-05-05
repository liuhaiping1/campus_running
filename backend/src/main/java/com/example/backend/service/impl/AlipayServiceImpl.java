package com.example.backend.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.backend.config.AlipayConfig;
import com.example.backend.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    @Override
    public String buildPayForm(Long orderId, String orderNo, String amount, String subject) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl() + "?orderId=" + orderId);

        // 支付宝 biz_content JSON
        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                "\"total_amount\":\"%s\",\"subject\":\"%s\"}",
                orderNo, amount, subject
        );
        request.setBizContent(bizContent);

        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付表单失败: {}", e.getMessage(), e);
            throw new RuntimeException("支付请求创建失败，请稍后重试");
        }
    }

    @Override
    public boolean verifyNotifySign(HttpServletRequest request) {
        try {
            Map<String, String> params = extractParams(request);
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isTradeSuccess(HttpServletRequest request) {
        String tradeStatus = request.getParameter("trade_status");
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append((i == values.length - 1) ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }
        return params;
    }
}
