package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.OrderStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.EvaluationSubmitRequest;
import com.example.backend.entity.ErrandOrder;
import com.example.backend.entity.OrderEvaluation;
import com.example.backend.entity.OrderStatusLog;
import com.example.backend.mapper.ErrandOrderMapper;
import com.example.backend.mapper.OrderEvaluationMapper;
import com.example.backend.mapper.OrderStatusLogMapper;
import com.example.backend.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单评价服务实现类
 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final OrderEvaluationMapper orderEvaluationMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;

    /**
     * 提交订单评价
     *
     * @param userId  当前用户ID（发布人）
     * @param request 评价提交请求
     * @return 评价记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, EvaluationSubmitRequest request) {
        ErrandOrder order = errandOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }

        Integer orderStatus = order.getOrderStatus();
        if (orderStatus == null || !OrderStatusEnum.COMPLETED.getCode().equals(orderStatus)) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_EVALUATE);
        }
        if (order.getRunnerId() == null) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_EVALUATE);
        }

        LambdaQueryWrapper<OrderEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderEvaluation::getOrderId, request.getOrderId())
                .eq(OrderEvaluation::getIsDeleted, 0);
        Long existingCount = orderEvaluationMapper.selectCount(queryWrapper);
        if (existingCount > 0) {
            throw new BusinessException(ErrorCode.EVALUATION_ALREADY_EXISTS);
        }

        OrderEvaluation evaluation = new OrderEvaluation();
        evaluation.setOrderId(request.getOrderId());
        evaluation.setPublisherId(userId);
        evaluation.setRunnerId(order.getRunnerId());
        evaluation.setStarScore(request.getScore());
        evaluation.setContent(request.getContent());
        evaluation.setIsAnonymous(0);
        try {
            orderEvaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.EVALUATION_ALREADY_EXISTS);
        }

        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(request.getOrderId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setBeforeStatus(OrderStatusEnum.COMPLETED.getCode());
        statusLog.setAfterStatus(OrderStatusEnum.COMPLETED.getCode());
        statusLog.setTriggerAction("EVALUATE_ORDER");
        statusLog.setOperatorUserId(userId);
        statusLog.setOperatorRole("STUDENT");
        orderStatusLogMapper.insert(statusLog);

        return evaluation.getId();
    }
}
