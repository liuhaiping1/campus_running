package com.example.backend.controller.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.dto.request.OrderCreateRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.OrderService;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器（用户端）
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    /**
     * 构造函数注入订单服务
     * @param orderService 订单服务
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建跑腿订单
     * @param loginUser 当前登录用户
     * @param request   订单创建请求
     * @return 新订单ID
     */
    @PostMapping
    public Result<Long> create(@AuthenticationPrincipal LoginUser loginUser,
                               @Valid @RequestBody OrderCreateRequest request) {
        Long orderId = orderService.create(loginUser.getUserId(), request);
        return Result.success("订单创建成功", orderId);
    }

    /**
     * 查询我的订单列表
     * @param loginUser   当前登录用户
     * @param orderStatus 订单状态筛选，可选
     * @param payStatus   支付状态筛选，可选
     * @param pageNum     页码，默认1
     * @param pageSize    每页大小，默认10
     * @return 分页订单列表
     */
    @GetMapping
    public Result<IPage<OrderVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestParam(required = false) Integer orderStatus,
                                       @RequestParam(required = false) Integer payStatus,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        IPage<OrderVO> result = orderService.myOrders(loginUser.getUserId(),
                orderStatus, payStatus, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 查询订单详情
     * @param id        订单ID
     * @param loginUser 当前登录用户
     * @return 订单详情（含状态流转日志）
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        OrderDetailVO detail = orderService.detail(id, loginUser.getUserId());
        return Result.success(detail);
    }
}
