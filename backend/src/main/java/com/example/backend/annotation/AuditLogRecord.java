package com.example.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志记录注解
 * <p>
 * 标记在 Controller 方法上，由 {@code AuditLogAspect} 切面自动拦截
 * 并写入 audit_log 表。不标记则不记录。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogRecord {

    /** 模块名称，如 ORDER / AUTH / CATEGORY / NOTICE / APPEAL / REFUND */
    String module();

    /** 操作动作，如 CREATE / UPDATE / DELETE / ACCEPT / CANCEL / REVIEW / APPROVE / HANDLE */
    String action();

    /** 业务类型，如 ORDER / AUTH / APPEAL，默认空 */
    String bizType() default "";

    /** 业务标识字符串，直接写入 audit_log.biz_id，为空则不写 */
    String bizId() default "";

    /** 操作描述，成功时写入 resultMsg */
    String description() default "";
}
