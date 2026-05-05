package com.example.backend.vo;

import com.example.backend.entity.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 审计日志视图对象
 * <p>
 * 封装审计日志的展示信息，包含操作人、模块、请求路径、IP、结果等关键字段。
 * 不包含 requestParam 等大字段，避免列表加载过重。
 * </p>
 */
@Data
@Builder
public class AuditLogVO {

    /** 审计日志 resultCode 为 "0" 时表示业务成功 */
    private static final String RESULT_CODE_SUCCESS = "0";

    /** 主键ID */
    private Long id;

    /** 链路追踪号 */
    private String traceId;

    /** 模块名称 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 操作描述（操作类型 + 结果说明的复合描述） */
    private String operationDesc;

    /** 请求方法 */
    private String requestMethod;

    /** 请求路径 */
    private String requestUri;

    /** 操作人ID */
    private Long operatorUserId;

    /** 操作人角色 */
    private String operatorRole;

    /** 请求IP地址 */
    private String ipAddress;

    /** 操作是否成功（resultCode 为 "0" 表示成功） */
    private Boolean success;

    /** 失败时的错误信息，成功时为 null */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 接口耗时，单位毫秒 */
    private Long costTime;

    /**
     * 根据审计日志实体构建视图对象
     *
     * @param log 审计日志实体
     * @return 审计日志视图对象
     */
    public static AuditLogVO from(AuditLog log) {
        boolean isSuccess = RESULT_CODE_SUCCESS.equals(log.getResultCode());
        return AuditLogVO.builder()
                .id(log.getId())
                .traceId(log.getTraceId())
                .module(log.getModuleName())
                .action(log.getActionType())
                .operationDesc(log.getActionType() + " - " + Objects.toString(log.getResultMsg(), ""))
                .requestMethod(log.getRequestMethod())
                .requestUri(log.getRequestPath())
                .operatorUserId(log.getOperatorId())
                .operatorRole(log.getOperatorRole())
                .ipAddress(log.getIpAddress())
                .success(isSuccess)
                .errorMessage(isSuccess ? null : log.getResultMsg())
                .createTime(log.getCreateTime())
                .costTime(log.getCostTime())
                .build();
    }
}
