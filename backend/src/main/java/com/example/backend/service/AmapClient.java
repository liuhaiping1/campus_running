package com.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * 高德地图 Web 服务客户端接口
 * <p>
 * 封装高德地图 REST API 的基础调用能力，包括路线规划、地理编码和逆地理编码。
 * 所有方法返回高德接口的原始 JsonNode，由调用方按需解析。
 * </p>
 */
public interface AmapClient {

    /**
     * 步行路线规划
     *
     * @param originLng 起点经度
     * @param originLat 起点纬度
     * @param destLng   终点经度
     * @param destLat   终点纬度
     * @return 高德步行路线规划返回结果
     */
    JsonNode walkingRoute(BigDecimal originLng, BigDecimal originLat,
                          BigDecimal destLng, BigDecimal destLat);

    /**
     * 骑行路线规划
     *
     * @param originLng 起点经度
     * @param originLat 起点纬度
     * @param destLng   终点经度
     * @param destLat   终点纬度
     * @return 高德骑行路线规划返回结果
     */
    JsonNode bicyclingRoute(BigDecimal originLng, BigDecimal originLat,
                            BigDecimal destLng, BigDecimal destLat);

    /**
     * 地理编码（地址转坐标）
     *
     * @param address 结构化地址，如"北京市海淀区清华大学"
     * @param city    城市名称，可为空
     * @return 高德地理编码返回结果
     */
    JsonNode geocode(String address, String city);

    /**
     * 逆地理编码（坐标转地址）
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 高德逆地理编码返回结果
     */
    JsonNode regeo(BigDecimal lng, BigDecimal lat);
}
