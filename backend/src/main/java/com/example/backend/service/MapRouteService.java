package com.example.backend.service;

import com.example.backend.dto.request.RouteEstimateRequest;
import com.example.backend.vo.RouteEstimateVO;

/**
 * 地图路线服务接口
 */
public interface MapRouteService {

    /**
     * 路线预估：根据起终点经纬度计算路线距离、预计时间和价格明细
     *
     * @param request 路线预估请求
     * @return 路线预估结果，包含距离、时间、价格等信息
     */
    RouteEstimateVO estimate(RouteEstimateRequest request);
}
