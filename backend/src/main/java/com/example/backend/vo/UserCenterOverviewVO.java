package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 学生个人中心概览响应视图对象
 */
@Data
@Builder
public class UserCenterOverviewVO {

    /**
     * 个人资料
     */
    private UserProfileVO profile;

    /**
     * 总订单数
     */
    private Long totalOrderCount;

    /**
     * 待支付订单数
     */
    private Long unpaidOrderCount;

    /**
     * 进行中订单数（待接单、已接单、已联系、已取件、配送中、已送达）
     */
    private Long ongoingOrderCount;

    /**
     * 已完成订单数
     */
    private Long completedOrderCount;

    /**
     * 已取消订单数
     */
    private Long cancelledOrderCount;

    /**
     * 申诉中订单数
     */
    private Long appealOrderCount;

    /**
     * 未读消息数
     */
    private Long unreadMessageCount;

    /**
     * 跑腿员认证状态：null表示无认证记录
     */
    private Integer runnerAuthStatus;
}
