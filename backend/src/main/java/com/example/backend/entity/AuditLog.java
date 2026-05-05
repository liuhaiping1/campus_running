package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务标识
     */
    private String bizId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人角色
     */
    private String operatorRole;

    /**
     * 操作类型
     */
    private String actionType;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求参数摘要
     */
    private String requestParam;

    /**
     * 处理结果码
     */
    private String resultCode;

    /**
     * 处理结果说明
     */
    private String resultMsg;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 链路追踪号
     */
    private String traceId;

    /**
     * 接口耗时，单位毫秒
     */
    private Long costTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer isDeleted;
}
