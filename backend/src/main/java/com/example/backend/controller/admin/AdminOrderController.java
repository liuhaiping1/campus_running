package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.service.AdminOrderService;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理员订单管理控制器
 * <p>
 * 提供管理端全量订单查询和订单详情查看接口，需要ADMIN角色。
 * 路由受 {@code /api/admin/**} 的 SecurityConfig 保护。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * 构造函数注入订单管理服务
     *
     * @param adminOrderService 订单管理服务
     */
    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /**
     * 分页查询全量订单列表（管理端）
     * <p>
     * 支持按订单状态、支付状态、结算状态、关键词（匹配订单号或标题）、
     * 创建时间范围进行筛选。结果按创建时间倒序排列。
     * 管理员不受订单所属人限制。
     * </p>
     *
     * @param orderStatus     订单状态筛选，可选
     * @param payStatus       支付状态筛选，可选
     * @param settlementStatus 结算状态筛选，可选
     * @param keyword         关键词（模糊匹配订单号或标题），可选
     * @param startTime       创建时间起始（格式 yyyy-MM-dd HH:mm:ss），可选
     * @param endTime         创建时间截止（格式 yyyy-MM-dd HH:mm:ss），可选
     * @param pageNum         页码，默认1
     * @param pageSize        每页大小，默认10
     * @return 分页订单列表
     */
    @GetMapping("/list")
    public Result<IPage<OrderVO>> list(@RequestParam(required = false) Integer orderStatus,
                                       @RequestParam(required = false) Integer payStatus,
                                       @RequestParam(required = false) Integer settlementStatus,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false)
                                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                       @RequestParam(required = false)
                                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        IPage<OrderVO> result = adminOrderService.list(
                orderStatus, payStatus, settlementStatus,
                keyword, startTime, endTime,
                pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 查询任意订单详情（管理端）
     * <p>
     * 管理员可查看任意订单的完整信息，包括分类名称和状态流转日志，
     * 不受发布人或接单人权限限制。订单不存在时返回错误。
     * </p>
     *
     * @param id 订单ID
     * @return 订单详情（含状态流转日志）
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        OrderDetailVO detail = adminOrderService.detail(id);
        return Result.success(detail);
    }
}
