package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.dto.request.OrderCancelRequest;
import com.example.backend.dto.request.OrderCreateRequest;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderVO;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建跑腿订单
     * @param userId  发布人ID
     * @param request 订单创建请求
     * @return 订单ID
     */
    Long create(Long userId, OrderCreateRequest request);

    /**
     * 查询我的订单（发布或接单）
     * @param userId      当前用户ID
     * @param orderStatus 订单状态筛选，可选
     * @param payStatus   支付状态筛选，可选
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 分页订单列表
     */
    IPage<OrderVO> myOrders(Long userId, Integer orderStatus, Integer payStatus, int pageNum, int pageSize);

    /**
     * 查询订单详情
     * @param orderId 订单ID
     * @param userId  当前用户ID
     * @return 订单详情（含状态日志）
     */
    OrderDetailVO detail(Long orderId, Long userId);

    /**
     * 查询任务大厅（跑腿员可接的订单）
     * @param userId    当前用户ID
     * @param categoryId 分类筛选，可选
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页订单列表
     */
    IPage<OrderVO> hall(Long userId, Long categoryId, int pageNum, int pageSize);

    /**
     * 跑腿员接单
     * @param orderId   订单ID
     * @param runnerId  跑腿员ID
     */
    void accept(Long orderId, Long runnerId);

    /**
     * 跑腿员确认已联系发布人
     * @param orderId  订单ID
     * @param runnerId 跑腿员ID
     */
    void contact(Long orderId, Long runnerId);

    /**
     * 跑腿员确认已取货
     * @param orderId  订单ID
     * @param runnerId 跑腿员ID
     */
    void pickup(Long orderId, Long runnerId);

    /**
     * 跑腿员确认已送达
     * @param orderId  订单ID
     * @param runnerId 跑腿员ID
     */
    void deliver(Long orderId, Long runnerId);

    /**
     * 发布人确认订单完成
     * @param orderId 订单ID
     * @param userId  发布人ID
     */
    void complete(Long orderId, Long userId);

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId  当前用户ID（发布人或跑腿员）
     * @param request 取消请求
     */
    void cancel(Long orderId, Long userId, OrderCancelRequest request);
}
