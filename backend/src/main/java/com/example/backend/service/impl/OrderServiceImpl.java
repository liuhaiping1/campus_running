package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.util.FeeRuleUtil;
import com.example.backend.common.enums.AuthStatusEnum;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.enums.PayStatusEnum;
import com.example.backend.common.enums.RefundStatusEnum;
import com.example.backend.common.enums.SettlementStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.OrderCancelRequest;
import com.example.backend.dto.request.OrderCreateRequest;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.ErrandOrderAddress;
import com.example.backend.entity.ErrandOrderDetail;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.entity.OrderEvaluation;
import com.example.backend.entity.PaymentOrder;
import com.example.backend.entity.RefundRecord;
import com.example.backend.entity.RunnerAuth;
import com.example.backend.entity.RunnerIncomeRecord;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.ErrandOrderAddressMapper;
import com.example.backend.mapper.ErrandOrderDetailMapper;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.PaymentOrderMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.RefundRecordMapper;
import com.example.backend.mapper.RunnerAuthMapper;
import com.example.backend.mapper.RunnerIncomeRecordMapper;
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
import java.util.Map;
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
    private static final String CONTACT_ORDER_ACTION = "CONTACT_ORDER";
    private static final String PICKUP_ORDER_ACTION = "PICKUP_ORDER";
    private static final String DELIVER_ORDER_ACTION = "DELIVER_ORDER";
    private static final String COMPLETE_ORDER_ACTION = "COMPLETE_ORDER";
    private static final String CANCEL_ORDER_ACTION = "CANCEL_ORDER";

    /**
     * 状态码到时间字段名的映射，用于条件更新时设置对应时间戳
     */
    private static final Map<Integer, String> STATUS_TIME_FIELD_MAP = Map.of(
            OrderStatusEnum.CONTACTED.getCode(), "contact_time",
            OrderStatusEnum.PICKED_UP.getCode(), "pickup_time",
            OrderStatusEnum.DELIVERED.getCode(), "deliver_time",
            OrderStatusEnum.COMPLETED.getCode(), "complete_time"
    );

    private final ErrandOrderMapper errandOrderMapper;
    private final ErrandCategoryMapper errandCategoryMapper;
    private final ErrandOrderAddressMapper errandOrderAddressMapper;
    private final ErrandOrderDetailMapper errandOrderDetailMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderEvaluationMapper orderEvaluationMapper;
    private final RunnerAuthMapper runnerAuthMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final RunnerIncomeRecordMapper runnerIncomeRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数注入Mapper
     */
    public OrderServiceImpl(ErrandOrderMapper errandOrderMapper,
                            ErrandCategoryMapper errandCategoryMapper,
                            ErrandOrderAddressMapper errandOrderAddressMapper,
                            ErrandOrderDetailMapper errandOrderDetailMapper,
                            PaymentOrderMapper paymentOrderMapper,
                            OrderStatusLogMapper orderStatusLogMapper,
                            OrderEvaluationMapper orderEvaluationMapper,
                            RunnerAuthMapper runnerAuthMapper,
                            RefundRecordMapper refundRecordMapper,
                            RunnerIncomeRecordMapper runnerIncomeRecordMapper) {
        this.errandOrderMapper = errandOrderMapper;
        this.errandCategoryMapper = errandCategoryMapper;
        this.errandOrderAddressMapper = errandOrderAddressMapper;
        this.errandOrderDetailMapper = errandOrderDetailMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.orderEvaluationMapper = orderEvaluationMapper;
        this.runnerAuthMapper = runnerAuthMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.runnerIncomeRecordMapper = runnerIncomeRecordMapper;
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
        BigDecimal distanceFee = FeeRuleUtil.calculateDistanceFee(objectMapper, category.getDistanceFeeRule(), request.getDistanceKm());
        BigDecimal weightFee = BigDecimal.ZERO;
        BigDecimal timeFee = BigDecimal.ZERO;
        BigDecimal tipFee = request.getTipFee() != null ? request.getTipFee() : BigDecimal.ZERO;
        BigDecimal orderAmount = baseFee.add(distanceFee).add(weightFee).add(timeFee).add(tipFee);
        BigDecimal platformCommission = BigDecimal.ZERO;
        BigDecimal estimatedRunnerIncome = orderAmount;

        LocalDateTime now = LocalDateTime.now();
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
        // 费用快照
        order.setFeeRuleVersion(category.getFeeRuleVersion());
        order.setFeeDetail(buildFeeDetailJson(baseFee, distanceFee, weightFee, timeFee, tipFee,
                orderAmount, platformCommission, estimatedRunnerIncome, request.getDistanceKm(),
                category.getFeeRuleVersion()));
        // 附件与联系人
        order.setAttachmentUrls(request.getAttachmentUrls());
        order.setContactName(request.getContactName() != null ? request.getContactName()
                : request.getPickupContactName());
        order.setContactPhone(request.getContactPhone() != null ? request.getContactPhone()
                : request.getPickupContactPhone());
        // 距离兜底字段（不接地图API）
        order.setStraightDistanceKm(request.getDistanceKm());
        order.setRouteDistanceKm(null);
        order.setRouteDurationSec(null);
        order.setDistanceSource(2); // 2=直线兜底，不接地图时用 straightDistanceKm
        boolean coordsComplete = request.getPickupLng() != null && request.getPickupLat() != null
                && request.getDeliveryLng() != null && request.getDeliveryLat() != null;
        order.setDistanceCalcStatus(coordsComplete ? 1 : 0);
        order.setMapProvider(9);
        order.setRouteStrategy(request.getRouteStrategy());
        order.setDistanceCalcTime(coordsComplete ? now : null);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        errandOrderMapper.insert(order);

        // 写入起点地址快照
        insertOrderAddress(order.getId(), order.getOrderNo(), now, userId,
                1, // addressRole = 起点
                request.getPickupAddressSource(),
                request.getPickupSourceRefId(),
                request.getPickupMapPoiId(),
                request.getPickupContactName(),
                request.getPickupContactPhone(),
                request.getPickupCampusName(),
                request.getPickupBuildingName(),
                request.getPickupAddress(),
                request.getPickupFormattedAddress(),
                request.getPickupProvinceName(),
                request.getPickupCityName(),
                request.getPickupDistrictName(),
                request.getPickupAdcode(),
                request.getPickupLng(),
                request.getPickupLat());

        // 写入终点地址快照
        insertOrderAddress(order.getId(), order.getOrderNo(), now, userId,
                2, // addressRole = 终点
                request.getDeliveryAddressSource(),
                request.getDeliverySourceRefId(),
                request.getDeliveryMapPoiId(),
                request.getDeliveryContactName(),
                request.getDeliveryContactPhone(),
                request.getDeliveryCampusName(),
                request.getDeliveryBuildingName(),
                request.getDeliveryAddress(),
                request.getDeliveryFormattedAddress(),
                request.getDeliveryProvinceName(),
                request.getDeliveryCityName(),
                request.getDeliveryDistrictName(),
                request.getDeliveryAdcode(),
                request.getDeliveryLng(),
                request.getDeliveryLat());

        // 写入分类扩展详情
        ErrandOrderDetail orderDetail = new ErrandOrderDetail();
        orderDetail.setOrderId(order.getId());
        orderDetail.setOrderNo(order.getOrderNo());
        // categoryCode 从已查询的分类实体获取，不从前端请求传入
        orderDetail.setCategoryCode(category.getCategoryCode());
        // 快递字段
        orderDetail.setExpressCompany(request.getExpressCompany());
        orderDetail.setExpressStation(request.getExpressStation());
        orderDetail.setExpressNo(request.getExpressNo());
        orderDetail.setExpressPickupCode(request.getExpressPickupCode());
        orderDetail.setExpressPhoneSuffix(request.getExpressPhoneSuffix());
        orderDetail.setPackageCount(request.getPackageCount() != null ? request.getPackageCount() : 1);
        orderDetail.setPackageWeightKg(request.getPackageWeightKg());
        orderDetail.setPackageSize(request.getPackageSize());
        // 外卖字段
        orderDetail.setTakeawayPlatform(request.getTakeawayPlatform());
        orderDetail.setTakeawayOrderNo(request.getTakeawayOrderNo());
        orderDetail.setTakeawayPickupCode(request.getTakeawayPickupCode());
        orderDetail.setTakeawayPhoneSuffix(request.getTakeawayPhoneSuffix());
        orderDetail.setMerchantName(request.getMerchantName());
        orderDetail.setMerchantPhone(request.getMerchantPhone());
        orderDetail.setFoodItemCount(request.getFoodItemCount());
        orderDetail.setExpectedPickupTime(request.getExpectedPickupTime());
        orderDetail.setNeedInsulation(request.getNeedInsulation() != null ? request.getNeedInsulation() : 0);
        // 代买字段
        orderDetail.setShoppingItems(request.getShoppingItems());
        orderDetail.setShoppingBudget(request.getShoppingBudget());
        orderDetail.setAllowPriceAdjust(request.getAllowPriceAdjust() != null ? request.getAllowPriceAdjust() : 0);
        // 资料字段
        orderDetail.setDocumentName(request.getDocumentName());
        orderDetail.setDocumentCount(request.getDocumentCount());
        orderDetail.setDocumentRemark(request.getDocumentRemark());
        // 帮办字段
        orderDetail.setHelpType(request.getHelpType());
        orderDetail.setHelpContent(request.getHelpContent());
        orderDetail.setCreateTime(now);
        orderDetail.setUpdateTime(now);
        orderDetail.setCreateBy(userId);
        orderDetail.setUpdateBy(userId);
        errandOrderDetailMapper.insert(orderDetail);

        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setBeforeStatus(null);
        statusLog.setAfterStatus(OrderStatusEnum.UNPAID.getCode());
        statusLog.setTriggerAction(CREATE_ORDER_ACTION);
        statusLog.setOperatorUserId(userId);
        statusLog.setOperatorRole(OPERATOR_ROLE_STUDENT);
        statusLog.setCreateTime(now);
        statusLog.setUpdateTime(now);

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
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .createTime(baseVO.getCreateTime())
                .updateTime(baseVO.getUpdateTime())
                .statusLogs(logVOs)
                .build();

        // 查询地址快照，查询不到不抛异常
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

        // 查询评价ID，前端据此隐藏已评价订单的评价入口。
        LambdaQueryWrapper<OrderEvaluation> evaluationWrapper = new LambdaQueryWrapper<>();
        evaluationWrapper.eq(OrderEvaluation::getOrderId, orderId);
        OrderEvaluation evaluation = orderEvaluationMapper.selectOne(evaluationWrapper);
        if (evaluation != null) {
            detailVO.setEvaluationId(evaluation.getId());
        }

        return detailVO;
    }

    /**
     * 插入订单地址快照
     * <p>
     * 将订单的起点或终点地址信息写入 errand_order_address 表。
     * 经纬度都不为空时 geocodeStatus=1（解析成功），否则为 0（未解析）。
     * </p>
     */
    private void insertOrderAddress(Long orderId, String orderNo, LocalDateTime now, Long userId,
                                    Integer addressRole, Integer addressSource, Long sourceRefId,
                                    String mapPoiId, String contactName, String contactPhone,
                                    String campusName, String buildingName, String detailAddress,
                                    String formattedAddress, String provinceName, String cityName,
                                    String districtName, String adcode,
                                    java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
        ErrandOrderAddress addr = new ErrandOrderAddress();
        addr.setOrderId(orderId);
        addr.setAddressRole(addressRole);
        addr.setAddressSource(addressSource != null ? addressSource : 1);
        addr.setMapProvider(9); // 9 = 系统兜底，未接地图
        addr.setMapPoiId(mapPoiId);
        addr.setSourceRefId(sourceRefId);
        addr.setContactName(contactName);
        addr.setContactPhone(contactPhone);
        addr.setCampusName(campusName);
        addr.setBuildingName(buildingName);
        addr.setDetailAddress(detailAddress);
        addr.setFormattedAddress(formattedAddress);
        addr.setProvinceName(provinceName);
        addr.setCityName(cityName);
        addr.setDistrictName(districtName);
        addr.setAdcode(adcode);
        addr.setLongitude(longitude);
        addr.setLatitude(latitude);
        addr.setCoordType("GCJ02");
        // 经纬度都不为空 → 解析成功，否则未解析
        if (longitude != null && latitude != null) {
            addr.setGeocodeStatus(1);
            addr.setGeocodeTime(now);
        } else {
            addr.setGeocodeStatus(0);
            addr.setGeocodeTime(null);
        }
        addr.setCreateTime(now);
        addr.setUpdateTime(now);
        addr.setCreateBy(userId);
        addr.setUpdateBy(userId);
        errandOrderAddressMapper.insert(addr);
    }

    /**
     * 构建费用计算明细 JSON 字符串，使用 Jackson 保证字符正确转义
     */
    private String buildFeeDetailJson(BigDecimal baseFee, BigDecimal distanceFee, BigDecimal weightFee,
                                       BigDecimal timeFee, BigDecimal tipFee, BigDecimal orderAmount,
                                       BigDecimal platformCommission, BigDecimal estimatedRunnerIncome,
                                       BigDecimal distanceKm, String feeRuleVersion) {
        try {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("baseFee", baseFee != null ? baseFee.toPlainString() : null);
            map.put("distanceFee", distanceFee != null ? distanceFee.toPlainString() : null);
            map.put("weightFee", weightFee != null ? weightFee.toPlainString() : null);
            map.put("timeFee", timeFee != null ? timeFee.toPlainString() : null);
            map.put("tipFee", tipFee != null ? tipFee.toPlainString() : null);
            map.put("orderAmount", orderAmount != null ? orderAmount.toPlainString() : null);
            map.put("platformCommission", platformCommission != null ? platformCommission.toPlainString() : null);
            map.put("estimatedRunnerIncome", estimatedRunnerIncome != null ? estimatedRunnerIncome.toPlainString() : null);
            map.put("distanceKm", distanceKm != null ? distanceKm.toPlainString() : null);
            map.put("feeRuleVersion", feeRuleVersion);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
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

        // 2. 如 categoryId 不为空，添加 categoryId 筛选
        if (categoryId != null) {
            wrapper.eq(ErrandOrder::getCategoryId, categoryId);
        }

        wrapper.orderByDesc(ErrandOrder::getCreateTime);
        Page<ErrandOrder> resultPage = errandOrderMapper.selectPage(pageParam, wrapper);

        // 3. 分页查询并返回 OrderVO 列表
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
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<ErrandOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .isNull("runner_id")
                .eq("order_status", OrderStatusEnum.WAITING_ACCEPT.getCode())
                .eq("pay_status", PayStatusEnum.PAID.getCode())
                .set("runner_id", runnerId)
                .set("order_status", OrderStatusEnum.ACCEPTED.getCode())
                .set("accept_time", now)
                .set("update_time", now);

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
        statusLog.setCreateTime(now);
        statusLog.setUpdateTime(now);

        orderStatusLogMapper.insert(statusLog);
    }

    /**
     * 跑腿员联系用户（已接单 → 已联系用户）
     */
    @Transactional(rollbackFor = Exception.class)
    public void contact(Long orderId, Long runnerId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!runnerId.equals(order.getRunnerId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
        updateOrderStatus(orderId, OrderStatusEnum.ACCEPTED.getCode(), OrderStatusEnum.CONTACTED.getCode(),
                runnerId, "runner_id", LocalDateTime.now());
        insertStatusLog(orderId, order.getOrderNo(),
                OrderStatusEnum.ACCEPTED.getCode(), OrderStatusEnum.CONTACTED.getCode(),
                CONTACT_ORDER_ACTION, runnerId, OPERATOR_ROLE_RUNNER);
    }

    /**
     * 跑腿员取件（已联系用户 → 已取件）
     */
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Long orderId, Long runnerId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!runnerId.equals(order.getRunnerId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
        updateOrderStatus(orderId, OrderStatusEnum.CONTACTED.getCode(), OrderStatusEnum.PICKED_UP.getCode(),
                runnerId, "runner_id", LocalDateTime.now());
        insertStatusLog(orderId, order.getOrderNo(),
                OrderStatusEnum.CONTACTED.getCode(), OrderStatusEnum.PICKED_UP.getCode(),
                PICKUP_ORDER_ACTION, runnerId, OPERATOR_ROLE_RUNNER);
    }

    /**
     * 跑腿员送达（已取件 → 已送达）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long orderId, Long runnerId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!runnerId.equals(order.getRunnerId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
        updateOrderStatus(orderId, OrderStatusEnum.PICKED_UP.getCode(), OrderStatusEnum.DELIVERED.getCode(),
                runnerId, "runner_id", LocalDateTime.now());
        insertStatusLog(orderId, order.getOrderNo(),
                OrderStatusEnum.PICKED_UP.getCode(), OrderStatusEnum.DELIVERED.getCode(),
                DELIVER_ORDER_ACTION, runnerId, OPERATOR_ROLE_RUNNER);
    }

    /**
     * 用户确认完成（已送达 → 已完成）
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId, Long userId) {
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getPublisherId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
        // 检查收益记录是否已存在，防止重复生成
        LambdaQueryWrapper<RunnerIncomeRecord> incomeCheckWrapper = new LambdaQueryWrapper<>();
        incomeCheckWrapper.eq(RunnerIncomeRecord::getOrderId, orderId)
                .eq(RunnerIncomeRecord::getIsDeleted, 0);
        long existingCount = runnerIncomeRecordMapper.selectCount(incomeCheckWrapper);
        updateOrderStatus(orderId, OrderStatusEnum.DELIVERED.getCode(), OrderStatusEnum.COMPLETED.getCode(),
                userId, "publisher_id", LocalDateTime.now());
        insertStatusLog(orderId, order.getOrderNo(),
                OrderStatusEnum.DELIVERED.getCode(), OrderStatusEnum.COMPLETED.getCode(),
                COMPLETE_ORDER_ACTION, userId, OPERATOR_ROLE_STUDENT);
        // 仅在更新成功且收益记录不存在时创建
        if (existingCount == 0) {
            RunnerIncomeRecord incomeRecord = new RunnerIncomeRecord();
            incomeRecord.setOrderId(orderId);
            incomeRecord.setRunnerId(order.getRunnerId());
            incomeRecord.setIncomeAmount(order.getEstimatedRunnerIncome());
            incomeRecord.setSettlementStatus(SettlementStatusEnum.PENDING.getCode());
            incomeRecord.setCreateTime(LocalDateTime.now());
            incomeRecord.setUpdateTime(incomeRecord.getCreateTime());
            runnerIncomeRecordMapper.insert(incomeRecord);
        }
    }

    /**
     * 取消订单
     * <p>
     * 发布人或跑腿员可取消订单，取消后订单不可再操作。
     * 已支付订单取消后创建退款记录（不做真实退款）。
     * </p>
     * 取消规则：
     * <ul>
     *   <li>发布人可取消：UNPAID、WAITING_ACCEPT、ACCEPTED、CONTACTED、PICKED_UP</li>
     *   <li>跑腿员可取消：ACCEPTED、CONTACTED、PICKED_UP</li>
     *   <li>DELIVERED、COMPLETED、CANCELLED、APPEALING 不可取消</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId, OrderCancelRequest request) {
        // 1. 查询订单
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 校验权限：发布人或跑腿员
        boolean isPublisher = userId.equals(order.getPublisherId());
        boolean isRunner = userId.equals(order.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }

        // 3. 校验取消状态
        Integer currentStatus = order.getOrderStatus();
        if (isPublisher) {
            // 发布人：UNPAID(0)、WAITING_ACCEPT(1)、ACCEPTED(2)、CONTACTED(3)、PICKED_UP(4) 可取消
            if (currentStatus.equals(OrderStatusEnum.UNPAID.getCode()) ||
                currentStatus.equals(OrderStatusEnum.WAITING_ACCEPT.getCode()) ||
                currentStatus.equals(OrderStatusEnum.ACCEPTED.getCode()) ||
                currentStatus.equals(OrderStatusEnum.CONTACTED.getCode()) ||
                currentStatus.equals(OrderStatusEnum.PICKED_UP.getCode())) {
                // 可取消
            } else {
                throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
            }
        } else {
            // 跑腿员：ACCEPTED(2)、CONTACTED(3)、PICKED_UP(4) 可取消
            if (currentStatus.equals(OrderStatusEnum.ACCEPTED.getCode()) ||
                currentStatus.equals(OrderStatusEnum.CONTACTED.getCode()) ||
                currentStatus.equals(OrderStatusEnum.PICKED_UP.getCode())) {
                // 可取消
            } else {
                throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
            }
        }

        // 4. 条件更新订单为取消状态
        UpdateWrapper<ErrandOrder> uw = new UpdateWrapper<>();
        uw.eq("id", orderId)
          .eq("order_status", currentStatus);
        if (isPublisher) {
            uw.eq("publisher_id", userId);
        } else {
            uw.eq("runner_id", userId);
        }
        LocalDateTime now = LocalDateTime.now();
        uw.set("order_status", OrderStatusEnum.CANCELLED.getCode())
          .set("cancel_time", now)
          .set("update_time", now)
          .set("cancel_reason", request.getCancelReason())
          .set("cancel_user_id", userId)
          .set("cancel_role", isPublisher ? OPERATOR_ROLE_STUDENT : OPERATOR_ROLE_RUNNER);

        int rows = errandOrderMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }

        // 5. 写入状态日志
        insertStatusLog(orderId, order.getOrderNo(),
                currentStatus, OrderStatusEnum.CANCELLED.getCode(),
                CANCEL_ORDER_ACTION, userId, isPublisher ? OPERATOR_ROLE_STUDENT : OPERATOR_ROLE_RUNNER);

        // 6. 如果已支付，创建退款记录
        if (PayStatusEnum.PAID.getCode().equals(order.getPayStatus())) {
            RefundRecord refund = new RefundRecord();
            refund.setOrderId(orderId);
            refund.setRequestId("REF_REQ" + IdWorker.getIdStr());
            refund.setRefundNo("REF" + IdWorker.getIdStr());
            refund.setApplyUserId(userId);
            refund.setRefundType(1);
            refund.setRefundAmount(order.getOrderAmount());
            refund.setRefundReason(request.getCancelReason());
            refund.setRefundStatus(RefundStatusEnum.PENDING.getCode());
            // 关联支付单号快照
            LambdaQueryWrapper<PaymentOrder> payWrapper = new LambdaQueryWrapper<>();
            payWrapper.eq(PaymentOrder::getOrderId, orderId);
            PaymentOrder payOrder = paymentOrderMapper.selectOne(payWrapper);
            refund.setPayNo(payOrder != null ? payOrder.getPayNo() : null);
            refund.setCreateTime(LocalDateTime.now());
            refund.setUpdateTime(refund.getCreateTime());
            refundRecordMapper.insert(refund);
        }
    }

    /**
     * 校验用户是否为已认证跑腿员
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

    /**
     * 更新订单状态（条件更新）
     */
    private void updateOrderStatus(Long orderId, Integer fromStatus, Integer toStatus,
                                   Long operatorId, String operatorField, LocalDateTime actionTime) {
        UpdateWrapper<ErrandOrder> uw = new UpdateWrapper<>();
        uw.eq("id", orderId)
          .eq("order_status", fromStatus)
          .eq(operatorField, operatorId);

        if (actionTime != null) {
            String timeField = STATUS_TIME_FIELD_MAP.get(toStatus);
            if (timeField != null) {
                uw.set(timeField, actionTime);
            }
        }
        uw.set("order_status", toStatus)
          .set("update_time", LocalDateTime.now());

        int rows = errandOrderMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
    }

    /**
     * 写入订单状态流转日志
     */
    private void insertStatusLog(Long orderId, String orderNo,
                                 Integer beforeStatus, Integer afterStatus,
                                 String triggerAction, Long operatorUserId, String operatorRole) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(orderId);
        statusLog.setOrderNo(orderNo);
        statusLog.setBeforeStatus(beforeStatus);
        statusLog.setAfterStatus(afterStatus);
        statusLog.setTriggerAction(triggerAction);
        statusLog.setOperatorUserId(operatorUserId);
        statusLog.setOperatorRole(operatorRole);
        statusLog.setCreateTime(LocalDateTime.now());
        statusLog.setUpdateTime(statusLog.getCreateTime());
        orderStatusLogMapper.insert(statusLog);
    }
}
