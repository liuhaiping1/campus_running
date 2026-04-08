package com.example.backend.service;

import com.example.backend.dto.request.EvaluationSubmitRequest;

/**
 * 订单评价服务接口
 */
public interface EvaluationService {

    /**
     * 提交订单评价
     * @param userId 当前用户ID（发布人）
     * @param request 评价提交请求
     * @return 评价记录ID
     */
    Long submit(Long userId, EvaluationSubmitRequest request);
}
