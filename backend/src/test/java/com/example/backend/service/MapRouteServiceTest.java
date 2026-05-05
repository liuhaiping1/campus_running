package com.example.backend.service;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.RouteEstimateRequest;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.MapRouteCalcLog;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.MapRouteCalcLogMapper;
import com.example.backend.service.impl.MapRouteServiceImpl;
import com.example.backend.vo.RouteEstimateVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MapRouteService 单元测试
 * <p>
 * 使用 Mockito 对 {@link MapRouteServiceImpl} 进行单元测试，
 * 覆盖高德骑行成功、步行成功、高德失败兜底、分类不存在、路线策略非法五种场景。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MapRouteService 单元测试")
class MapRouteServiceTest {

    @Mock
    private AmapClient amapClient;

    @Mock
    private ErrandCategoryMapper errandCategoryMapper;

    @Mock
    private MapRouteCalcLogMapper mapRouteCalcLogMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MapRouteServiceImpl mapRouteService;

    @Captor
    private ArgumentCaptor<MapRouteCalcLog> logCaptor;

    private ErrandCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new ErrandCategory();
        testCategory.setId(1L);
        testCategory.setCategoryName("快递代取");
        testCategory.setCategoryCode("EXPRESS");
        testCategory.setBaseFee(BigDecimal.valueOf(3));
        // 距离规则：0-1km 0元，1-3km 2元，3km以上 5元
        testCategory.setDistanceFeeRule("[{\"min\":0,\"max\":1,\"fee\":0},{\"min\":1,\"max\":3,\"fee\":2},{\"min\":3,\"max\":null,\"fee\":5}]");
        testCategory.setUrgentFee(BigDecimal.valueOf(2));
        testCategory.setFeeRuleVersion("v1.0");
        testCategory.setCategoryStatus(1);
    }

    private RouteEstimateRequest buildRequest() {
        RouteEstimateRequest req = new RouteEstimateRequest();
        req.setCategoryId(1L);
        req.setOriginLng(BigDecimal.valueOf(116.397428));
        req.setOriginLat(BigDecimal.valueOf(39.90923));
        req.setDestinationLng(BigDecimal.valueOf(116.407428));
        req.setDestinationLat(BigDecimal.valueOf(39.91923));
        req.setRouteStrategy("bicycling");
        req.setTipFee(BigDecimal.valueOf(1));
        return req;
    }

    /**
     * 模拟高德骑行 v4 返回成功结果
     */
    private JsonNode mockBicyclingSuccess() throws Exception {
        String json = "{\"status\":\"1\",\"data\":{\"paths\":[{\"distance\":\"1500\",\"duration\":\"300\"}]}}";
        JsonNode node = objectMapper.readTree(json);
        when(amapClient.bicyclingRoute(any(), any(), any(), any())).thenReturn(node);
        return node;
    }

    /**
     * 模拟高德步行 v3 返回成功结果
     */
    private JsonNode mockWalkingSuccess() throws Exception {
        String json = "{\"status\":\"1\",\"route\":{\"paths\":[{\"distance\":\"1200\",\"duration\":\"600\"}]}}";
        JsonNode node = objectMapper.readTree(json);
        when(amapClient.walkingRoute(any(), any(), any(), any())).thenReturn(node);
        return node;
    }

    // =========================================================================
    // 测试用例
    // =========================================================================

    @Nested
    @DisplayName("高德骑行成功场景")
    class BicyclingSuccessTests {

        @Test
        @DisplayName("骑行成功：应返回路线距离、mapProvider=1、distanceSource=1")
        void shouldReturnRouteDistance_whenBicyclingSucceeds() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            mockBicyclingSuccess();
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            RouteEstimateVO vo = mapRouteService.estimate(buildRequest());

            // 验证距离计算正确：1500m / 1000 = 1.50km
            assertEquals(0, BigDecimal.valueOf(1.50).compareTo(vo.getRouteDistanceKm()));
            assertEquals(300, vo.getRouteDurationSec());
            assertEquals(1, vo.getDistanceSource());
            assertEquals(1, vo.getDistanceCalcStatus());
            assertEquals(1, vo.getMapProvider());
            assertEquals("bicycling", vo.getRouteStrategy());
            assertNull(vo.getMessage());
        }

        @Test
        @DisplayName("骑行成功：应记录 map_route_calc_log")
        void shouldInsertCalcLog_whenBicyclingSucceeds() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            mockBicyclingSuccess();
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            mapRouteService.estimate(buildRequest());

            verify(mapRouteCalcLogMapper).insert(logCaptor.capture());
            MapRouteCalcLog logEntry = logCaptor.getValue();
            assertEquals(1, logEntry.getMapProvider());
            assertEquals("bicycling", logEntry.getRouteStrategy());
            assertEquals(1, logEntry.getCalcStatus());
            assertEquals(1500, logEntry.getRouteDistanceM());
            assertNotNull(logEntry.getRequestId());
            assertNull(logEntry.getOrderId());
        }

        @Test
        @DisplayName("骑行成功：应正确计算价格")
        void shouldCalculatePrice_whenBicyclingSucceeds() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            mockBicyclingSuccess();
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            RouteEstimateVO vo = mapRouteService.estimate(buildRequest());

            // baseFee=3, distanceFee=2 (1.5km在1-3区间), urgentFee=2, tipFee=1
            // orderAmount=3+2+2+1=8, commission=0.80, income=7.20
            assertEquals(0, BigDecimal.valueOf(3).compareTo(vo.getBaseFee()));
            assertEquals(0, BigDecimal.valueOf(2).compareTo(vo.getDistanceFee()));
            assertEquals(0, BigDecimal.valueOf(2).compareTo(vo.getUrgentFee()));
            assertEquals(0, BigDecimal.valueOf(1).compareTo(vo.getTipFee()));
            assertEquals(0, BigDecimal.valueOf(8).compareTo(vo.getOrderAmount()));
            assertEquals(0, BigDecimal.valueOf(0.80).compareTo(vo.getPlatformCommission()));
            assertEquals(0, BigDecimal.valueOf(7.20).compareTo(vo.getEstimatedRunnerIncome()));
            assertNotNull(vo.getFeeDetail());
            assertEquals("v1.0", vo.getFeeRuleVersion());
            assertNotNull(vo.getRequestId());
        }
    }

    @Nested
    @DisplayName("高德步行成功场景")
    class WalkingSuccessTests {

        @Test
        @DisplayName("步行成功：应解析 v3 route.paths[0] 结构")
        void shouldParseV3Response_whenWalkingSucceeds() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            mockWalkingSuccess();
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            RouteEstimateRequest req = buildRequest();
            req.setRouteStrategy("walking");
            RouteEstimateVO vo = mapRouteService.estimate(req);

            // 1200m / 1000 = 1.20km
            assertEquals(0, BigDecimal.valueOf(1.20).compareTo(vo.getRouteDistanceKm()));
            assertEquals(600, vo.getRouteDurationSec());
            assertEquals("walking", vo.getRouteStrategy());
            assertEquals(1, vo.getMapProvider());
        }
    }

    @Nested
    @DisplayName("高德失败兜底场景")
    class AmapFailureFallbackTests {

        @Test
        @DisplayName("高德失败：应使用直线距离兜底，mapProvider=9")
        void shouldFallbackToStraightDistance_whenAmapFails() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(amapClient.bicyclingRoute(any(), any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.AMAP_SERVICE_ERROR, "模拟失败"));
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            RouteEstimateVO vo = mapRouteService.estimate(buildRequest());

            assertNull(vo.getRouteDistanceKm());
            assertNull(vo.getRouteDurationSec());
            assertEquals(2, vo.getDistanceSource());
            assertEquals(2, vo.getDistanceCalcStatus());
            assertEquals(9, vo.getMapProvider());
            assertNotNull(vo.getMessage());
            assertTrue(vo.getMessage().contains("直线距离兜底"));
        }

        @Test
        @DisplayName("高德失败：仍应返回价格（基于直线距离）")
        void shouldReturnPrice_whenAmapFails() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(amapClient.bicyclingRoute(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("网络异常"));
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            RouteEstimateVO vo = mapRouteService.estimate(buildRequest());

            assertNotNull(vo.getOrderAmount());
            assertTrue(vo.getOrderAmount().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(vo.getBaseFee());
            assertNotNull(vo.getDistanceFee());
        }

        @Test
        @DisplayName("高德失败：应记录 map_route_calc_log，calcStatus=2")
        void shouldInsertCalcLogWithFallbackStatus_whenAmapFails() throws Exception {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);
            when(amapClient.bicyclingRoute(any(), any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.AMAP_SERVICE_ERROR));
            when(mapRouteCalcLogMapper.insert(any(MapRouteCalcLog.class))).thenReturn(1);

            mapRouteService.estimate(buildRequest());

            verify(mapRouteCalcLogMapper).insert(logCaptor.capture());
            MapRouteCalcLog logEntry = logCaptor.getValue();
            assertEquals(9, logEntry.getMapProvider());
            assertEquals(2, logEntry.getCalcStatus());
            assertNull(logEntry.getRouteDistanceM());
            assertNotNull(logEntry.getStraightDistanceM());
            assertNotNull(logEntry.getErrorMsg());
        }
    }

    @Nested
    @DisplayName("参数校验场景")
    class ValidationTests {

        @Test
        @DisplayName("分类不存在：应抛出 CATEGORY_NOT_FOUND")
        void shouldThrow_whenCategoryNotFound() {
            when(errandCategoryMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mapRouteService.estimate(buildRequest()));
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("分类已停用：应抛出 CATEGORY_NOT_FOUND")
        void shouldThrow_whenCategoryDisabled() {
            testCategory.setCategoryStatus(2);
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mapRouteService.estimate(buildRequest()));
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("路线策略非法：应抛出 BAD_REQUEST")
        void shouldThrow_whenRouteStrategyInvalid() {
            when(errandCategoryMapper.selectById(1L)).thenReturn(testCategory);

            RouteEstimateRequest req = buildRequest();
            req.setRouteStrategy("driving");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mapRouteService.estimate(req));
            assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        }
    }
}
