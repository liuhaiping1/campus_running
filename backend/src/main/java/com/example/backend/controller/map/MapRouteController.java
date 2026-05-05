package com.example.backend.controller.map;

import com.example.backend.common.Result;
import com.example.backend.dto.request.RouteEstimateRequest;
import com.example.backend.service.MapRouteService;
import com.example.backend.vo.RouteEstimateVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地图路线控制器
 */
@RestController
@RequestMapping("/api/map/route")
public class MapRouteController {

    private final MapRouteService mapRouteService;

    public MapRouteController(MapRouteService mapRouteService) {
        this.mapRouteService = mapRouteService;
    }

    /**
     * 路线预估：根据起终点经纬度计算路线距离、预计时间和价格明细
     *
     * @param request 路线预估请求
     * @return 路线预估结果
     */
    @PostMapping("/estimate")
    public Result<RouteEstimateVO> estimate(@Valid @RequestBody RouteEstimateRequest request) {
        RouteEstimateVO vo = mapRouteService.estimate(request);
        return Result.success(vo);
    }
}
