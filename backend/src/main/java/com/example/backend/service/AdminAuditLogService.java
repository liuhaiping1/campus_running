package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.vo.AuditLogVO;

import java.time.LocalDateTime;

/**
 * 管理员审计日志服务接口
 * <p>
 * 提供管理端审计日志的分页查询能力，仅ADMIN角色可访问。
 * 支持按模块、操作动作、操作人、角色、traceId、关键词和时间范围筛选。
 * </p>
 */
public interface AdminAuditLogService {

    /**
     * 分页查询审计日志（管理端）
     * <p>
     * 支持按模块、动作、操作人ID、操作人角色、traceId 精确筛选，
     * 以及关键词模糊匹配请求路径、请求方法、IP地址、操作描述，
     * 和按创建时间范围过滤。结果按创建时间倒序。
     * </p>
     *
     * @param module         模块名称精确筛选，可选
     * @param action         操作动作精确筛选，可选
     * @param operatorUserId 操作人ID精确筛选，可选
     * @param operatorRole   操作人角色精确筛选，可选
     * @param traceId        链路追踪号精确筛选，可选
     * @param keyword        关键词（模糊匹配请求URI/方法/IP/描述），可选
     * @param startTime      创建时间起始，可选
     * @param endTime        创建时间截止，可选
     * @param pageNum        页码，默认1
     * @param pageSize       每页大小，默认10
     * @return 分页审计日志列表
     */
    IPage<AuditLogVO> list(String module, String action, Long operatorUserId,
                           String operatorRole, String traceId, String keyword,
                           LocalDateTime startTime, LocalDateTime endTime,
                           int pageNum, int pageSize);
}
