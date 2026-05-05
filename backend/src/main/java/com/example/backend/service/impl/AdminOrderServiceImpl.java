package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.ErrandOrderAddress;
import com.example.backend.entity.ErrandOrderDetail;
import com.example.backend.entity.OrderEvaluation;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.ErrandOrderAddressMapper;
import com.example.backend.mapper.ErrandOrderDetailMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.service.AdminOrderService;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderStatusLogVO;
import com.example.backend.vo.OrderVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员订单服务实现类
 * <p>
 * 提供管理端全量订单查询能力。与用户端 OrderService 的区别在于：
 * 管理员可查看所有订单，不受发布人或接单人权限限制，
 * 同时支持更丰富的筛选条件（结算状态、关键词、时间范围）。
 * </p>
 */
@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final ErrandOrderMapper errandOrderMapper;
    private final ErrandCategoryMapper errandCategoryMapper;
    private final ErrandOrderAddressMapper errandOrderAddressMapper;
    private final ErrandOrderDetailMapper errandOrderDetailMapper;
    private final OrderEvaluationMapper orderEvaluationMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;

    /**
     * 构造函数注入Mapper
     */
    public AdminOrderServiceImpl(ErrandOrderMapper errandOrderMapper,
                                  ErrandCategoryMapper errandCategoryMapper,
                                  ErrandOrderAddressMapper errandOrderAddressMapper,
                                  ErrandOrderDetailMapper errandOrderDetailMapper,
                                  OrderEvaluationMapper orderEvaluationMapper,
                                  OrderStatusLogMapper orderStatusLogMapper) {
        this.errandOrderMapper = errandOrderMapper;
        this.errandCategoryMapper = errandCategoryMapper;
        this.errandOrderAddressMapper = errandOrderAddressMapper;
        this.errandOrderDetailMapper = errandOrderDetailMapper;
        this.orderEvaluationMapper = orderEvaluationMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
    }

    /**
     * 分页查询全量订单列表（管理端）
     * <p>
     * 支持按订单状态、支付状态、结算状态筛选；关键词对订单号（orderNo）或标题（title）
     * 进行模糊匹配；时间范围筛选 createTime 区间。结果按 createTime 倒序。
     * 管理员不受订单所属人限制。
     * </p>
     *
     * @param orderStatus      订单状态筛选
     * @param payStatus        支付状态筛选
     * @param settlementStatus 结算状态筛选
     * @param keyword          关键词（匹配订单号或标题）
     * @param startTime        创建时间起始
     * @param endTime          创建时间截止
     * @param pageNum          页码
     * @param pageSize         每页大小
     * @return 分页订单列表
     */
    @Override
    public IPage<OrderVO> list(Integer orderStatus, Integer payStatus, Integer settlementStatus,
                               String keyword, LocalDateTime startTime, LocalDateTime endTime,
                               int pageNum, int pageSize) {
        Page<ErrandOrder> pageParam = new Page<>(pageNum, pageSize);

        // 构建查询条件：管理员不用限制 publisherId 或 runnerId
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();

        // 按订单状态筛选
        if (orderStatus != null) {
            wrapper.eq(ErrandOrder::getOrderStatus, orderStatus);
        }
        // 按支付状态筛选
        if (payStatus != null) {
            wrapper.eq(ErrandOrder::getPayStatus, payStatus);
        }
        // 按结算状态筛选
        if (settlementStatus != null) {
            wrapper.eq(ErrandOrder::getSettlementStatus, settlementStatus);
        }
        // 关键词模糊匹配订单号或标题
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> w.like(ErrandOrder::getOrderNo, trimmedKeyword)
                    .or()
                    .like(ErrandOrder::getTitle, trimmedKeyword));
        }
        // 创建时间范围筛选
        if (startTime != null) {
            wrapper.ge(ErrandOrder::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(ErrandOrder::getCreateTime, endTime);
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(ErrandOrder::getCreateTime);

        Page<ErrandOrder> resultPage = errandOrderMapper.selectPage(pageParam, wrapper);

        // 收集所有分类ID，批量查询分类名称，避免N+1
        List<Long> categoryIds = resultPage.getRecords().stream()
                .map(ErrandOrder::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> categoryNameMap = new java.util.HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<ErrandCategory> categories = errandCategoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(ErrandCategory::getId, ErrandCategory::getCategoryName, (a, b) -> a));
        }

        List<OrderVO> voList = new ArrayList<>();
        for (ErrandOrder order : resultPage.getRecords()) {
            String categoryName = categoryNameMap.getOrDefault(order.getCategoryId(), null);
            voList.add(OrderVO.from(order, categoryName));
        }

        Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 查询任意订单详情（管理端）
     * <p>
     * 管理员查看订单详情时，仅校验订单存在性，不校验发布人或接单人权限。
     * 详情包含订单完整信息、分类名称和状态流转日志。
     * </p>
     *
     * @param orderId 订单ID
     * @return 订单详情（含状态流转日志）
     * @throws BusinessException 订单不存在时抛出 ORDER_NOT_FOUND
     */
    @Override
    public OrderDetailVO detail(Long orderId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 管理员不做所有权校验，直接查询详情
        ErrandCategory category = errandCategoryMapper.selectById(order.getCategoryId());
        String categoryName = category != null ? category.getCategoryName() : null;

        // 查询状态流转日志，按创建时间升序
        LambdaQueryWrapper<OrderStatusLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getCreateTime);
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(logWrapper);
        List<OrderStatusLogVO> logVOs = logs.stream()
                .map(OrderStatusLogVO::from)
                .collect(Collectors.toList());

        // 组装详情 VO
        OrderVO baseVO = OrderVO.from(order, categoryName);
        OrderDetailVO detailVO = OrderDetailVO.builder()
                .id(baseVO.getId())
                .orderNo(baseVO.getOrderNo())
                .publisherId(baseVO.getPublisherId())
                .runnerId(baseVO.getRunnerId())
                .categoryId(baseVO.getCategoryId())
                .categoryName(baseVO.getCategoryName())
                .title(baseVO.getTitle())
                .orderDesc(baseVO.getOrderDesc())
                .pickupAddress(baseVO.getPickupAddress())
                .deliveryAddress(baseVO.getDeliveryAddress())
                .pickupLng(baseVO.getPickupLng())
                .pickupLat(baseVO.getPickupLat())
                .deliveryLng(baseVO.getDeliveryLng())
                .deliveryLat(baseVO.getDeliveryLat())
                .distanceKm(baseVO.getDistanceKm())
                .baseFee(baseVO.getBaseFee())
                .distanceFee(baseVO.getDistanceFee())
                .weightFee(baseVO.getWeightFee())
                .timeFee(baseVO.getTimeFee())
                .tipFee(baseVO.getTipFee())
                .orderAmount(baseVO.getOrderAmount())
                .platformCommission(baseVO.getPlatformCommission())
                .estimatedRunnerIncome(baseVO.getEstimatedRunnerIncome())
                .orderStatus(baseVO.getOrderStatus())
                .payStatus(baseVO.getPayStatus())
                .settlementStatus(baseVO.getSettlementStatus())
                .deadlineTime(baseVO.getDeadlineTime())
                .acceptTime(baseVO.getAcceptTime())
                .contactTime(baseVO.getContactTime())
                .pickupTime(baseVO.getPickupTime())
                .deliverTime(baseVO.getDeliverTime())
                .completeTime(baseVO.getCompleteTime())
                .cancelTime(baseVO.getCancelTime())
                .cancelReason(baseVO.getCancelReason())
                .appealFlag(baseVO.getAppealFlag())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .createTime(baseVO.getCreateTime())
                .updateTime(baseVO.getUpdateTime())
                .statusLogs(logVOs)
                .build();

        // 查询地址快照，查不到不抛异常
        LambdaQueryWrapper<ErrandOrderAddress> addrWrapper = new LambdaQueryWrapper<>();
        addrWrapper.eq(ErrandOrderAddress::getOrderId, orderId);
        List<ErrandOrderAddress> addresses = errandOrderAddressMapper.selectList(addrWrapper);
        for (ErrandOrderAddress addr : addresses) {
            if (Integer.valueOf(1).equals(addr.getAddressRole())) {
                detailVO.setPickupAddressDetail(addr);
            } else if (Integer.valueOf(2).equals(addr.getAddressRole())) {
                detailVO.setDeliveryAddressDetail(addr);
            }
        }

        // 查询分类扩展详情，查不到不抛异常
        LambdaQueryWrapper<ErrandOrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(ErrandOrderDetail::getOrderId, orderId);
        ErrandOrderDetail orderDetail = errandOrderDetailMapper.selectOne(detailWrapper);
        detailVO.setOrderDetail(orderDetail);

        // 查询评价ID，保持管理端和普通端详情结构一致。
        LambdaQueryWrapper<OrderEvaluation> evaluationWrapper = new LambdaQueryWrapper<>();
        evaluationWrapper.eq(OrderEvaluation::getOrderId, orderId);
        OrderEvaluation evaluation = orderEvaluationMapper.selectOne(evaluationWrapper);
        if (evaluation != null) {
            detailVO.setEvaluationId(evaluation.getId());
        }

        return detailVO;
    }
}
