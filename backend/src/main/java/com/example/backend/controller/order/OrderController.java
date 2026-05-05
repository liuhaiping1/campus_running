package com.example.backend.controller.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.annotation.AuditLogRecord;
import com.example.backend.common.Result;
import com.example.backend.dto.request.OrderCancelRequest;
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
    @AuditLogRecord(module = "ORDER", action = "CREATE", bizType = "ORDER", description = "创建订单")
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

    /**
     * 查询任务大厅（跑腿员可接的订单）
     * @param loginUser   当前登录用户（跑腿员）
     * @param categoryId  分类筛选，可选
     * @param pageNum     页码，默认1
     * @param pageSize    每页大小，默认10
     * @return 分页订单列表
     */
    @GetMapping("/hall")
    public Result<IPage<OrderVO>> hall(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        IPage<OrderVO> result = orderService.hall(loginUser.getUserId(), categoryId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 跑腿员接单
     * @param id        订单ID
     * @param loginUser 当前登录用户（跑腿员）
     * @return 操作结果
     */
    @AuditLogRecord(module = "ORDER", action = "ACCEPT", bizType = "ORDER", description = "接单")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        orderService.accept(id, loginUser.getUserId());
        return Result.success("接单成功", null);
    }

    /**
     * 跑腿员确认已联系发布人
     * @param id        订单ID
     * @param loginUser 当前登录用户（跑腿员）
     * @return 操作结果
     */
    @AuditLogRecord(module = "ORDER", action = "CONTACT", bizType = "ORDER", description = "联系发布人")
    @PostMapping("/{id}/contact")
    public Result<Void> contact(@PathVariable Long id,
                                @AuthenticationPrincipal LoginUser loginUser) {
        orderService.contact(id, loginUser.getUserId());
        return Result.success("联系成功", null);
    }

    /**
     * 跑腿员确认已取货
     * @param id        订单ID
     * @param loginUser 当前登录用户（跑腿员）
     * @return 操作结果
     */
    @AuditLogRecord(module = "ORDER", action = "PICKUP", bizType = "ORDER", description = "取件")
    @PostMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        orderService.pickup(id, loginUser.getUserId());
        return Result.success("取货成功", null);
    }

    /**
     * 跑腿员确认已送达
     * @param id        订单ID
     * @param loginUser 当前登录用户（跑腿员）
     * @return 操作结果
     */
    @AuditLogRecord(module = "ORDER", action = "DELIVER", bizType = "ORDER", description = "送达")
    @PostMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id,
                                @AuthenticationPrincipal LoginUser loginUser) {
        orderService.deliver(id, loginUser.getUserId());
        return Result.success("送达成功", null);
    }

    /**
     * 发布人确认订单完成
     * @param id        订单ID
     * @param loginUser 当前登录用户（发布人）
     * @return 操作结果
     */
    @AuditLogRecord(module = "ORDER", action = "COMPLETE", bizType = "ORDER", description = "确认完成")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        orderService.complete(id, loginUser.getUserId());
        return Result.success("确认完成", null);
    }

    /**
     * 取消订单
     * @param id        订单ID
     * @param loginUser 当前登录用户
     * @param request   取消请求
     * @return 操作结果
     */
    @AuditLogRecord(module = "ORDER", action = "CANCEL", bizType = "ORDER", description = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser,
                               @Valid @RequestBody OrderCancelRequest request) {
        orderService.cancel(id, loginUser.getUserId(), request);
        return Result.success("取消成功", null);
    }
}
