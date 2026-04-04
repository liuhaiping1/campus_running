package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.enums.SettlementStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.OrderCreateRequest;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.service.OrderService;
import com.example.backend.vo.OrderDetailVO;
import com.example.backend.vo.OrderStatusLogVO;
import com.example.backend.vo.OrderVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final String CREATE_ORDER_ACTION = "CREATE_ORDER";
    private static final String OPERATOR_ROLE_STUDENT = "STUDENT";
    private static final String ACCEPT_ORDER_ACTION = "ACCEPT_ORDER";
    private static final String OPERATOR_ROLE_RUNNER = "RUNNER";

    private final ErrandOrderMapper errandOrderMapper;
    private final ErrandCategoryMapper errandCategoryMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final RunnerAuthMapper runnerAuthMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数注入Mapper
     */
    public OrderServiceImpl(ErrandOrderMapper errandOrderMapper,
                            ErrandCategoryMapper errandCategoryMapper,
                            OrderStatusLogMapper orderStatusLogMapper,
                            RunnerAuthMapper runnerAuthMapper) {
        this.errandOrderMapper = errandOrderMapper;
        this.errandCategoryMapper = errandCategoryMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.runnerAuthMapper = runnerAuthMapper;
    }

    /**
     * 创建跑腿订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, OrderCreateRequest request) {
        ErrandCategory category = errandCategoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(category.getCategoryStatus())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "该分类已停用");
        }

        BigDecimal baseFee = category.getBaseFee() != null ? category.getBaseFee() : BigDecimal.ZERO;
        BigDecimal distanceFee = calculateDistanceFee(category.getDistanceFeeRule(), request.getDistanceKm());
        BigDecimal weightFee = BigDecimal.ZERO;
        BigDecimal timeFee = BigDecimal.ZERO;
        BigDecimal tipFee = request.getTipFee() != null ? request.getTipFee() : BigDecimal.ZERO;
        BigDecimal orderAmount = baseFee.add(distanceFee).add(weightFee).add(timeFee).add(tipFee);
        BigDecimal platformCommission = BigDecimal.ZERO;
        BigDecimal estimatedRunnerIncome = orderAmount;

        String orderNo = "ER" + IdWorker.getIdStr();

        ErrandOrder order = new ErrandOrder();
        order.setOrderNo(orderNo);
        order.setPublisherId(userId);
        order.setRunnerId(null);
        order.setCategoryId(request.getCategoryId());
        order.setTitle(request.getTitle());
        order.setOrderDesc(request.getOrderDesc());
        order.setPickupAddress(request.getPickupAddress());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPickupLng(request.getPickupLng());
        order.setPickupLat(request.getPickupLat());
        order.setDeliveryLng(request.getDeliveryLng());
        order.setDeliveryLat(request.getDeliveryLat());
        order.setDistanceKm(request.getDistanceKm());
        order.setBaseFee(baseFee);
        order.setDistanceFee(distanceFee);
        order.setWeightFee(weightFee);
        order.setTimeFee(timeFee);
        order.setTipFee(tipFee);
        order.setOrderAmount(orderAmount);
        order.setPlatformCommission(platformCommission);
        order.setEstimatedRunnerIncome(estimatedRunnerIncome);
        order.setOrderStatus(OrderStatusEnum.UNPAID.getCode());
        order.setPayStatus(PayStatusEnum.UNPAID.getCode());
        order.setSettlementStatus(SettlementStatusEnum.PENDING.getCode());
        order.setDeadlineTime(request.getDeadlineTime());

        errandOrderMapper.insert(order);

        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setBeforeStatus(null);
        statusLog.setAfterStatus(OrderStatusEnum.UNPAID.getCode());
        statusLog.setTriggerAction(CREATE_ORDER_ACTION);
        statusLog.setOperatorUserId(userId);
        statusLog.setOperatorRole(OPERATOR_ROLE_STUDENT);

        orderStatusLogMapper.insert(statusLog);

        return order.getId();
    }

    /**
     * 查询我的订单
     */
    @Override
    public IPage<OrderVO> myOrders(Long userId, Integer orderStatus, Integer payStatus, int pageNum, int pageSize) {
        Page<ErrandOrder> pageParam = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ErrandOrder::getPublisherId, userId)
                .or()
                .eq(ErrandOrder::getRunnerId, userId));
        if (orderStatus != null) {
            wrapper.eq(ErrandOrder::getOrderStatus, orderStatus);
        }
        if (payStatus != null) {
            wrapper.eq(ErrandOrder::getPayStatus, payStatus);
        }
        wrapper.orderByDesc(ErrandOrder::getCreateTime);

        Page<ErrandOrder> resultPage = errandOrderMapper.selectPage(pageParam, wrapper);

        List<OrderVO> voList = new ArrayList<>();
        for (ErrandOrder order : resultPage.getRecords()) {
            ErrandCategory category = errandCategoryMapper.selectById(order.getCategoryId());
            String categoryName = category != null ? category.getCategoryName() : null;
            voList.add(OrderVO.from(order, categoryName));
        }

        Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 查询订单详情
     */
    @Override
    public OrderDetailVO detail(Long orderId, Long userId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!userId.equals(order.getPublisherId()) && !userId.equals(order.getRunnerId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }

        ErrandCategory category = errandCategoryMapper.selectById(order.getCategoryId());
        String categoryName = category != null ? category.getCategoryName() : null;

        LambdaQueryWrapper<OrderStatusLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getCreateTime);
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(logWrapper);
        List<OrderStatusLogVO> logVOs = logs.stream()
                .map(OrderStatusLogVO::from)
                .collect(Collectors.toList());

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
                .createTime(baseVO.getCreateTime())
                .updateTime(baseVO.getUpdateTime())
                .statusLogs(logVOs)
                .build();

        return detailVO;
    }

    /**
     * 根据距离收费规则JSON计算距离费用
     * <p>
     * 规则格式为JSON数组：[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},...]
     * max=null表示无上限。规则不匹配或JSON解析失败时抛出BusinessException。
     * </p>
     */
    private BigDecimal calculateDistanceFee(String ruleJson, BigDecimal distanceKm) {
        if (ruleJson == null || ruleJson.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "分类距离收费规则未配置");
        }

        try {
            JsonNode rules = objectMapper.readTree(ruleJson);
            if (!rules.isArray() || rules.size() == 0) {
                throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "距离收费规则格式错误");
            }

            if (distanceKm == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "预估距离不能为空");
            }

            for (JsonNode rule : rules) {
                JsonNode minNode = rule.get("min");
                JsonNode maxNode = rule.get("max");
                JsonNode feeNode = rule.get("fee");

                double min = minNode != null ? minNode.asDouble() : 0;
                Double max = (maxNode != null && !maxNode.isNull()) ? maxNode.asDouble() : null;
                double distanceValue = distanceKm.doubleValue();

                if (distanceValue >= min && (max == null || distanceValue < max)) {
                    return feeNode != null ? BigDecimal.valueOf(feeNode.asDouble()) : BigDecimal.ZERO;
                }
            }

            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE,
                    "距离" + distanceKm + "km未匹配到收费规则");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "距离收费规则解析失败");
        }
    }

    /**
     * 查询任务大厅（跑腿员可接的订单）
     * <p>
     * 只展示满足以下条件的订单：
     * <ul>
     *   <li>orderStatus = 待接单状态（WAITING_ACCEPT）</li>
     *   <li>payStatus = 已支付状态（PAID）</li>
     *   <li>runnerId 为空（未被接单）</li>
     *   <li>未逻辑删除</li>
     * </ul>
     * 同时校验当前用户是已认证跑腿员。
     */
    @Override
    public IPage<OrderVO> hall(Long userId, Long categoryId, int pageNum, int pageSize) {
        validateRunnerAuth(userId);

        // 1. 构建查询条件：orderStatus=WAITING_ACCEPT, payStatus=PAID, runnerId=null
        Page<ErrandOrder> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandOrder::getOrderStatus, OrderStatusEnum.WAITING_ACCEPT.getCode())
                .eq(ErrandOrder::getPayStatus, PayStatusEnum.PAID.getCode())
                .isNull(ErrandOrder::getRunnerId);

        // 3. 如 categoryId 不为空，添加 categoryId 筛选
        if (categoryId != null) {
            wrapper.eq(ErrandOrder::getCategoryId, categoryId);
        }

        wrapper.orderByDesc(ErrandOrder::getCreateTime);
        Page<ErrandOrder> resultPage = errandOrderMapper.selectPage(pageParam, wrapper);

        // 4. 分页查询并返回 OrderVO 列表
        List<OrderVO> voList = new ArrayList<>();
        for (ErrandOrder order : resultPage.getRecords()) {
            ErrandCategory category = errandCategoryMapper.selectById(order.getCategoryId());
            String categoryName = category != null ? category.getCategoryName() : null;
            voList.add(OrderVO.from(order, categoryName));
        }

        Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 跑腿员接单
     * <p>
     * 接单前校验：
     * <ul>
     *   <li>订单存在</li>
     *   <li>订单未被接单（runnerId 为空）</li>
     *   <li>订单已支付</li>
     *   <li>订单状态允许接单（WAITING_ACCEPT）</li>
     *   <li>不能接自己发布的订单</li>
     * </ul>
     * 接单成功后：
     * <ul>
     *   <li>设置 runnerId</li>
     *   <li>orderStatus 更新为已接单（ACCEPTED）</li>
     *   <li>acceptTime 设置当前时间</li>
     *   <li>写入 order_status_log，triggerAction="ACCEPT_ORDER"，operatorRole="RUNNER"</li>
     * </ul>
     * 必须使用条件更新（LambdaQueryWrapper + update）防止并发抢单，不允许只查后 updateById。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId, Long runnerId) {
        validateRunnerAuth(runnerId);

        // 1. 查询订单，校验：存在、未被接单、已支付、状态允许接单、不是自己发布的
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getRunnerId() != null) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_ACCEPTED);
        }
        if (!PayStatusEnum.PAID.getCode().equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAID);
        }
        if (!OrderStatusEnum.WAITING_ACCEPT.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_ACCEPT);
        }
        if (runnerId.equals(order.getPublisherId())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_ACCEPT_SELF);
        }

        // 3. 使用条件更新：UPDATE errand_order SET runner_id=?, order_status=?, accept_time=?
        //    WHERE id=? AND runner_id IS NULL AND order_status=? AND pay_status=?
        UpdateWrapper<ErrandOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .isNull("runner_id")
                .eq("order_status", OrderStatusEnum.WAITING_ACCEPT.getCode())
                .eq("pay_status", PayStatusEnum.PAID.getCode())
                .set("runner_id", runnerId)
                .set("order_status", OrderStatusEnum.ACCEPTED.getCode())
                .set("accept_time", LocalDateTime.now());

        int updatedRows = errandOrderMapper.update(null, updateWrapper);

        // 4. 如果更新行数为0，抛出 ORDER_ALREADY_ACCEPTED 异常
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_ACCEPTED);
        }

        // 5. 写入 order_status_log（beforeStatus=WAITING_ACCEPT, afterStatus=ACCEPTED, triggerAction="ACCEPT_ORDER", operatorRole="RUNNER"）
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(orderId);
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setBeforeStatus(OrderStatusEnum.WAITING_ACCEPT.getCode());
        statusLog.setAfterStatus(OrderStatusEnum.ACCEPTED.getCode());
        statusLog.setTriggerAction(ACCEPT_ORDER_ACTION);
        statusLog.setOperatorUserId(runnerId);
        statusLog.setOperatorRole(OPERATOR_ROLE_RUNNER);

        orderStatusLogMapper.insert(statusLog);
    }

    /**
     * 校验用户是否为已认证跑腿员
     * <p>
     * 查询 runner_auth 表，确认用户认证状态为 APPROVED 且为当前有效记录。
     * 不通过时抛出 ORDER_HALL_ACCESS_DENIED 异常。
     *
     * @param userId 用户ID
     */
    private void validateRunnerAuth(Long userId) {
        LambdaQueryWrapper<RunnerAuth> authWrapper = new LambdaQueryWrapper<>();
        authWrapper.eq(RunnerAuth::getUserId, userId)
                .eq(RunnerAuth::getAuthStatus, AuthStatusEnum.APPROVED.getCode())
                .eq(RunnerAuth::getCurrentFlag, 1);
        RunnerAuth runnerAuth = runnerAuthMapper.selectOne(authWrapper);
        if (runnerAuth == null) {
            throw new BusinessException(ErrorCode.ORDER_HALL_ACCESS_DENIED);
        }
    }
}
