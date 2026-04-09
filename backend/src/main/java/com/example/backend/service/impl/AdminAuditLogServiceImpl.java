package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.AuditLog;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.service.AdminAuditLogService;
import com.example.backend.vo.AuditLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员审计日志服务实现类
 * <p>
 * 提供管理端审计日志的分页查询实现。所有筛选条件均为可选，
 * 不做审计日志写入，只提供只读查询。
 * </p>
 */
@Service
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AdminAuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 分页查询审计日志
     * <p>
     * 所有筛选参数为可选，内部逐一判空拼接条件。
     * 结果按 createTime 倒序排列。
     * </p>
     */
    @Override
    public IPage<AuditLogVO> list(String module, String action, Long operatorUserId,
                                   String operatorRole, String traceId, String keyword,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   int pageNum, int pageSize) {
        Page<AuditLog> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (module != null && !module.trim().isEmpty()) {
            wrapper.eq(AuditLog::getModuleName, module.trim());
        }
        if (action != null && !action.trim().isEmpty()) {
            wrapper.eq(AuditLog::getActionType, action.trim());
        }
        if (operatorUserId != null) {
            wrapper.eq(AuditLog::getOperatorId, operatorUserId);
        }
        if (operatorRole != null && !operatorRole.trim().isEmpty()) {
            wrapper.eq(AuditLog::getOperatorRole, operatorRole.trim());
        }
        if (traceId != null && !traceId.trim().isEmpty()) {
            wrapper.eq(AuditLog::getTraceId, traceId.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(AuditLog::getRequestPath, k)
                    .or()
                    .like(AuditLog::getRequestMethod, k)
                    .or()
                    .like(AuditLog::getIpAddress, k)
                    .or()
                    .like(AuditLog::getResultMsg, k));
        }
        if (startTime != null) {
            wrapper.ge(AuditLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(AuditLog::getCreateTime);

        Page<AuditLog> resultPage = auditLogMapper.selectPage(pageParam, wrapper);

        List<AuditLogVO> voList = new ArrayList<>();
        for (AuditLog log : resultPage.getRecords()) {
            voList.add(AuditLogVO.from(log));
        }

        Page<AuditLogVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
