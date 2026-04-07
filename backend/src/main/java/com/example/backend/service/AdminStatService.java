package com.example.backend.service;

import com.example.backend.vo.AdminOverviewVO;

/**
 * 管理员统计服务接口
 * <p>
 * 提供管理端统计概览所需的数据查询，包括订单统计、金额汇总、用户统计和待处理事项计数。
 * </p>
 */
public interface AdminStatService {

    /**
     * 查询后台统计概览数据
     * <p>
     * 汇总订单、支付、退款、用户、认证、申诉等关键统计数据，
     * 所有数值字段均返回默认值，不会出现null。
     * </p>
     *
     * @return 统计概览视图对象
     */
    AdminOverviewVO getOverview();
}
