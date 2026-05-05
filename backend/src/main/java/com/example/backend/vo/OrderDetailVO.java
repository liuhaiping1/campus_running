package com.example.backend.vo;

import com.example.backend.entity.ErrandOrderAddress;
import com.example.backend.entity.ErrandOrderDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 订单详情视图对象
 * <p>
 * 继承订单视图对象的全部字段，并追加状态流转日志列表和地址快照，
 * 用于订单详情页展示。
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

    /**
     * 起点/取件点地址快照
     */
    private ErrandOrderAddress pickupAddressDetail;

    /**
     * 终点/送达点地址快照
     */
    private ErrandOrderAddress deliveryAddressDetail;

    /**
     * 订单联系人姓名
     */
    private String contactName;

    /**
     * 订单联系人手机号
     */
    private String contactPhone;

    /**
     * 分类扩展详情
     */
    private ErrandOrderDetail orderDetail;

    /**
     * 订单评价ID。
     */
    private Long evaluationId;
}
