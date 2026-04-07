package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderVO;

import java.time.LocalDateTime;

/**
 * 管理员订单服务接口
 * <p>
 * 提供管理端全量订单查询和订单详情查看能力，不受发布人或接单人限制。
 * </p>
 */
public interface AdminOrderService {

    /**
     * 分页查询全量订单列表（管理端）
     * <p>
     * 支持按订单状态、支付状态、结算状态、关键词（匹配订单号或标题）、
     * 创建时间范围进行筛选，结果按创建时间倒序排列。
     * </p>
     *
     * @param orderStatus     订单状态筛选，可选
     * @param payStatus       支付状态筛选，可选
     * @param settlementStatus 结算状态筛选，可选
     * @param keyword         关键词（模糊匹配订单号或标题），可选
     * @param startTime       创建时间起始，可选
     * @param endTime         创建时间截止，可选
     * @param pageNum         页码，默认1
     * @param pageSize        每页大小，默认10
     * @return 分页订单列表
     */
    IPage<OrderVO> list(Integer orderStatus, Integer payStatus, Integer settlementStatus,
                        String keyword, LocalDateTime startTime, LocalDateTime endTime,
                        int pageNum, int pageSize);

    /**
     * 查询任意订单详情（管理端）
     * <p>
     * 管理员可以查看任意订单的完整信息，包括订单基础信息、分类名称和状态流转日志，
     * 不受发布人或接单人权限限制。
     * </p>
     *
     * @param orderId 订单ID
     * @return 订单详情（含状态流转日志）
     */
    OrderDetailVO detail(Long orderId);
}
