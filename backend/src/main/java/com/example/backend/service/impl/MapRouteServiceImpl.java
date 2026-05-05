package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.util.FeeRuleUtil;
import com.example.backend.dto.request.RouteEstimateRequest;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.MapRouteCalcLog;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.MapRouteCalcLogMapper;
import com.example.backend.service.AmapClient;
import com.example.backend.service.MapRouteService;
import com.example.backend.vo.RouteEstimateVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地图路线服务实现类
 * <p>
 * 调用高德地图 API 计算路线距离和时间，结合分类计价规则计算费用。
 * 高德调用失败时使用直线距离兜底。
 * </p>
 */
@Slf4j
@Service
public class MapRouteServiceImpl implements MapRouteService {

    private static final int PROVIDER_AMAP = 1;
    private static final int PROVIDER_FALLBACK = 9;
    private static final int SOURCE_ROUTE = 1;
    private static final int SOURCE_STRAIGHT = 2;
    private static final int CALC_SUCCESS = 1;
    private static final int CALC_FALLBACK = 2;

    /** 平台抽成比例 */
    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.1);

    private final AmapClient amapClient;
    private final ErrandCategoryMapper errandCategoryMapper;
    private final MapRouteCalcLogMapper mapRouteCalcLogMapper;
    private final ObjectMapper objectMapper;

    public MapRouteServiceImpl(AmapClient amapClient,
                               ErrandCategoryMapper errandCategoryMapper,
                               MapRouteCalcLogMapper mapRouteCalcLogMapper,
                               ObjectMapper objectMapper) {
        this.amapClient = amapClient;
        this.errandCategoryMapper = errandCategoryMapper;
        this.mapRouteCalcLogMapper = mapRouteCalcLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RouteEstimateVO estimate(RouteEstimateRequest request) {
        // 1. 查询并校验分类
        ErrandCategory category = errandCategoryMapper.selectById(request.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getCategoryStatus())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. 校验路线策略
        String strategy = request.getRouteStrategy();
        if (strategy == null || strategy.isBlank()) {
            strategy = "bicycling";
        }
        if (!"walking".equals(strategy) && !"bicycling".equals(strategy)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "路线策略只支持 walking 或 bicycling");
        }

        // 3. 计算直线距离（haversine 公式）
        BigDecimal straightDistanceKm = haversine(
                request.getOriginLng(), request.getOriginLat(),
                request.getDestinationLng(), request.getDestinationLat());

        // 4. 调用高德路线规划
        RouteResult routeResult = callAmapRoute(strategy, request);

        // 5. 价格计算（计价距离：高德成功用路线距离，失败用直线距离）
        BigDecimal calcDistanceKm = routeResult.distanceKm != null
                ? routeResult.distanceKm : straightDistanceKm;
        BigDecimal baseFee = category.getBaseFee() != null ? category.getBaseFee() : BigDecimal.ZERO;
        BigDecimal distanceFee = FeeRuleUtil.calculateDistanceFee(
                objectMapper, category.getDistanceFeeRule(), calcDistanceKm);
        BigDecimal weightFee = BigDecimal.ZERO;
        BigDecimal timeFee = BigDecimal.ZERO;
        BigDecimal urgentFee = category.getUrgentFee() != null ? category.getUrgentFee() : BigDecimal.ZERO;
        BigDecimal tipFee = request.getTipFee() != null ? request.getTipFee() : BigDecimal.ZERO;
        BigDecimal orderAmount = baseFee.add(distanceFee).add(weightFee).add(timeFee).add(urgentFee).add(tipFee);
        BigDecimal platformCommission = orderAmount.multiply(COMMISSION_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedRunnerIncome = orderAmount.subtract(platformCommission);

        // 6. 构建费用明细 JSON
        String feeDetail = buildFeeDetailJson(baseFee, distanceFee, urgentFee, tipFee,
                orderAmount, platformCommission, estimatedRunnerIncome,
                calcDistanceKm, routeResult.source, routeResult.provider,
                category.getFeeRuleVersion());

        // 7. 记录 map_route_calc_log
        String requestId = IdWorker.getIdStr();
        saveCalcLog(requestId, strategy, request, routeResult, straightDistanceKm);

        // 8. 组装返回
        return RouteEstimateVO.builder()
                .straightDistanceKm(straightDistanceKm)
                .routeDistanceKm(routeResult.distanceKm)
                .routeDurationSec(routeResult.durationSec)
                .distanceSource(routeResult.source)
                .distanceCalcStatus(routeResult.status)
                .mapProvider(routeResult.provider)
                .routeStrategy(strategy)
                .baseFee(baseFee)
                .distanceFee(distanceFee)
                .weightFee(weightFee)
                .timeFee(timeFee)
                .urgentFee(urgentFee)
                .tipFee(tipFee)
                .orderAmount(orderAmount)
                .platformCommission(platformCommission)
                .estimatedRunnerIncome(estimatedRunnerIncome)
                .feeRuleVersion(category.getFeeRuleVersion())
                .feeDetail(feeDetail)
                .requestId(requestId)
                .message(routeResult.message)
                .build();
    }

    /**
     * 调用高德路线规划，失败时返回兜底结果
     */
    private RouteResult callAmapRoute(String strategy, RouteEstimateRequest request) {
        try {
            JsonNode result;
            String rootKey;
            if ("walking".equals(strategy)) {
                result = amapClient.walkingRoute(
                        request.getOriginLng(), request.getOriginLat(),
                        request.getDestinationLng(), request.getDestinationLat());
                rootKey = "route";
            } else {
                result = amapClient.bicyclingRoute(
                        request.getOriginLng(), request.getOriginLat(),
                        request.getDestinationLng(), request.getDestinationLat());
                rootKey = "data";
            }
            // 解析路线数据：v3 步行用 route.paths[0]，v4 骑行用 data.paths[0]
            JsonNode path = result.path(rootKey).path("paths").path(0);
            int distanceM = path.path("distance").asInt(-1);
            int durationS = path.path("duration").asInt(-1);
            if (distanceM <= 0 || durationS <= 0) {
                throw new BusinessException(ErrorCode.AMAP_SERVICE_ERROR, "高德返回的路线数据无效");
            }
            BigDecimal routeDistanceKm = BigDecimal.valueOf(distanceM)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            return new RouteResult(routeDistanceKm, durationS,
                    SOURCE_ROUTE, CALC_SUCCESS, PROVIDER_AMAP, null);
        } catch (Exception e) {
            // 高德失败，使用直线距离兜底
            log.warn("高德路线规划失败，使用直线距离兜底: {}", e.getMessage());
            return new RouteResult(null, null,
                    SOURCE_STRAIGHT, CALC_FALLBACK, PROVIDER_FALLBACK,
                    "地图路线计算失败，已使用直线距离兜底");
        }
    }

    /**
     * 保存路线计算日志，写入失败不影响主流程
     */
    private void saveCalcLog(String requestId, String strategy,
                             RouteEstimateRequest request, RouteResult routeResult,
                             BigDecimal straightDistanceKm) {
        try {
            LocalDateTime now = LocalDateTime.now();
            MapRouteCalcLog calcLog = new MapRouteCalcLog();
            calcLog.setRequestId(requestId);
            calcLog.setMapProvider(routeResult.provider);
            calcLog.setRouteStrategy(strategy);
            calcLog.setOriginLng(request.getOriginLng());
            calcLog.setOriginLat(request.getOriginLat());
            calcLog.setDestinationLng(request.getDestinationLng());
            calcLog.setDestinationLat(request.getDestinationLat());
            calcLog.setRouteDistanceM(routeResult.distanceKm != null
                    ? routeResult.distanceKm.multiply(BigDecimal.valueOf(1000)).intValue() : null);
            calcLog.setStraightDistanceM(straightDistanceKm.multiply(BigDecimal.valueOf(1000)).intValue());
            calcLog.setDurationSec(routeResult.durationSec);
            calcLog.setCalcStatus(routeResult.status);
            calcLog.setErrorMsg(routeResult.message);
            calcLog.setCreateTime(now);
            calcLog.setUpdateTime(now);
            mapRouteCalcLogMapper.insert(calcLog);
        } catch (Exception e) {
            // 日志写入失败不影响主流程
            log.warn("map_route_calc_log 写入失败: {}", e.getMessage());
        }
    }

    /**
     * Haversine 公式计算两点间直线距离（km），保留 2 位小数
     */
    private BigDecimal haversine(BigDecimal lng1, BigDecimal lat1,
                                 BigDecimal lng2, BigDecimal lat2) {
        double R = 6371.0; // 地球半径，单位 km
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(R * c).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 构建费用明细 JSON
     */
    private String buildFeeDetailJson(BigDecimal baseFee, BigDecimal distanceFee,
                                       BigDecimal urgentFee, BigDecimal tipFee,
                                       BigDecimal orderAmount, BigDecimal platformCommission,
                                       BigDecimal estimatedRunnerIncome, BigDecimal distanceKm,
                                       int distanceSource, int mapProvider, String feeRuleVersion) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("baseFee", baseFee.toPlainString());
            map.put("distanceFee", distanceFee.toPlainString());
            map.put("urgentFee", urgentFee.toPlainString());
            map.put("tipFee", tipFee.toPlainString());
            map.put("orderAmount", orderAmount.toPlainString());
            map.put("platformCommission", platformCommission.toPlainString());
            map.put("estimatedRunnerIncome", estimatedRunnerIncome.toPlainString());
            map.put("distanceKm", distanceKm.toPlainString());
            map.put("distanceSource", distanceSource);
            map.put("mapProvider", mapProvider);
            map.put("feeRuleVersion", feeRuleVersion);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 路线规划解析结果
     */
    private record RouteResult(BigDecimal distanceKm, Integer durationSec,
                               int source, int status, int provider, String message) {}
}
