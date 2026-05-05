package com.example.backend.service.impl;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.ErrorCode;
import com.example.backend.config.AmapConfig;
import com.example.backend.service.AmapClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 高德地图 Web 服务客户端实现
 * <p>
 * 基于 Spring RestTemplate 调用高德 REST API，自动拼接 key 和 output 参数。
 * 当 amap.enabled=false 时，所有调用直接抛出 BusinessException。
 * </p>
 */
@Slf4j
@Service
public class AmapClientImpl implements AmapClient {

    private final AmapConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AmapClientImpl(AmapConfig config, ObjectMapper objectMapper,
                          RestTemplateBuilder restTemplateBuilder) {
        this.config = config;
        this.objectMapper = objectMapper;
        // 配置 RestTemplate 超时时间
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public JsonNode walkingRoute(BigDecimal originLng, BigDecimal originLat,
                                 BigDecimal destLng, BigDecimal destLat) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("origin", toCoord(originLng, originLat));
        params.put("destination", toCoord(destLng, destLat));
        return callApi("/v3/direction/walking", params, "步行路线规划", this::parseV3Response);
    }

    @Override
    public JsonNode bicyclingRoute(BigDecimal originLng, BigDecimal originLat,
                                   BigDecimal destLng, BigDecimal destLat) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("origin", toCoord(originLng, originLat));
        params.put("destination", toCoord(destLng, destLat));
        return callApi("/v4/direction/bicycling", params, "骑行路线规划", this::parseV4Response);
    }

    @Override
    public JsonNode geocode(String address, String city) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("address", address);
        if (city != null && !city.isBlank()) {
            params.put("city", city);
        }
        return callApi("/v3/geocode/geo", params, "地理编码", this::parseV3Response);
    }

    @Override
    public JsonNode regeo(BigDecimal lng, BigDecimal lat) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("location", toCoord(lng, lat));
        return callApi("/v3/geocode/regeo", params, "逆地理编码", this::parseV3Response);
    }

    /**
     * 统一的高德 API 调用方法，自动拼接 key 和 output，解析响应
     *
     * @param path       API 路径，如 /v3/direction/walking
     * @param params     业务参数（不含 key 和 output）
     * @param apiName    接口中文名，用于日志和异常信息
     * @param parser     响应解析函数（v3/v4 策略不同）
     * @return 解析后的 JsonNode
     */
    private JsonNode callApi(String path, Map<String, String> params,
                             String apiName, Function<String, JsonNode> parser) {
        if (!config.isEnabled()) {
            throw new BusinessException(ErrorCode.AMAP_SERVICE_DISABLED);
        }

        // 构建绝对 URI，自动附加 key 和 output 参数
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(config.getBaseUrl())
                .path(path)
                .queryParam("key", config.getWebServiceKey())
                .queryParam("output", "JSON");
        params.forEach(builder::queryParam);
        URI uri = builder.build().toUri();

        // 日志只打印路径和业务参数，不暴露 key
        log.debug("高德{}请求: path={}, params={}", apiName, path, params);
        String body = restTemplate.getForObject(uri, String.class);
        return parser.apply(body);
    }

    /**
     * 将经度和纬度拼接为 "经度,纬度" 格式
     */
    private String toCoord(BigDecimal lng, BigDecimal lat) {
        return lng.toPlainString() + "," + lat.toPlainString();
    }

    /**
     * 解析高德 v3 接口返回，status=="1" 为成功
     */
    private JsonNode parseV3Response(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (!"1".equals(node.path("status").asText())) {
                String info = node.path("info").asText("未知错误");
                String infocode = node.path("infocode").asText("");
                throw new BusinessException(ErrorCode.AMAP_SERVICE_ERROR,
                        "高德地图服务调用失败: [" + infocode + "] " + info);
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AMAP_SERVICE_ERROR,
                    "高德地图服务响应解析失败");
        }
    }

    /**
     * 解析高德 v4 接口返回，errcode==0 为成功
     */
    private JsonNode parseV4Response(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.path("errcode").asInt(-1) != 0) {
                String errmsg = node.path("errmsg").asText("未知错误");
                throw new BusinessException(ErrorCode.AMAP_SERVICE_ERROR,
                        "高德地图服务调用失败: " + errmsg);
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AMAP_SERVICE_ERROR,
                    "高德地图服务响应解析失败");
        }
    }
}
