package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.security.LoginUser;
import com.example.backend.service.AlipayService;
import com.example.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PaymentService paymentService;
    private final AlipayService alipayService;

    /**
     * 发起支付 — 返回支付宝支付表单 HTML
     */
    @PostMapping("/{orderId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<String> pay(@PathVariable Long orderId,
                              @AuthenticationPrincipal LoginUser loginUser) {
        String payForm = paymentService.pay(orderId, loginUser.getUserId());
        return Result.success("请在新窗口中完成支付", payForm);
    }

    /**
     * 支付宝异步通知 — 验签后委托 Service 处理
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        if (!alipayService.verifyNotifySign(request)) {
            log.warn("支付宝回调验签失败");
            return "failure";
        }
        if (!alipayService.isTradeSuccess(request)) {
            log.warn("支付宝回调 trade_status 非成功状态");
            return "failure";
        }

        String orderNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        String totalAmount = request.getParameter("total_amount");
        log.info("支付宝支付成功回调: orderNo={}, tradeNo={}, amount={}", orderNo, tradeNo, totalAmount);

        return paymentService.handleNotify(orderNo, tradeNo, totalAmount);
    }
}
