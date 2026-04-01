package com.example.backend.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 订单详情视图对象
 * <p>
 * 继承订单视图对象的全部字段，并追加状态流转日志列表，用于订单详情页展示。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OrderDetailVO extends OrderVO {

    /**
     * 订单状态流转日志列表
     */
    private List<OrderStatusLogVO> statusLogs;
}
