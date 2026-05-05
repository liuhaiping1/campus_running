package com.example.backend.aspect;

import com.example.backend.annotation.AuditLogRecord;
import com.example.backend.entity.AuditLog;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 审计日志切面
 * <p>
 * 拦截带 {@code @AuditLogRecord} 注解的 Controller 方法，
 * 记录接口耗时和操作信息到 audit_log 表。
 * 审计写入失败不影响主业务流程。
 * </p>
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    /** 敏感参数字段名，记录时替换为 *** */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "token", "authorization",
            "certNo", "certFrontUrl", "certBackUrl"
    );

    /** requestParam 截断长度 */
    private static final int MAX_PARAM_LENGTH = 1000;

    private final AuditLogMapper auditLogMapper;

    public AuditLogAspect(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Around("@annotation(com.example.backend.annotation.AuditLogRecord)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditLogRecord annotation = getAnnotation(joinPoint);
        AuditLog auditLog = new AuditLog();
        auditLog.setCreateTime(LocalDateTime.now());

        // 当前用户信息
        Long operatorId = 0L;
        String operatorRole = "ANONYMOUS";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            operatorId = loginUser.getUserId();
            // 从 GrantedAuthority 取第一个角色名
            var authorities = loginUser.getAuthorities();
            if (authorities != null && !authorities.isEmpty()) {
                operatorRole = authorities.iterator().next().getAuthority();
            }
        }
        auditLog.setOperatorId(operatorId);
        auditLog.setOperatorRole(operatorRole);
        auditLog.setCreateBy(operatorId);
        auditLog.setUpdateBy(operatorId);

        // 注解字段
        auditLog.setModuleName(annotation.module());
        auditLog.setActionType(annotation.action());
        auditLog.setBizType(annotation.bizType());
        // bizId 直接使用注解字符串值
        if (!annotation.bizId().isEmpty()) {
            auditLog.setBizId(annotation.bizId());
        }

        // 请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            auditLog.setRequestPath(request.getRequestURI());
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setTraceId(getTraceId(request));
        }

        // 请求参数摘要
        auditLog.setRequestParam(buildParamSummary(joinPoint));

        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;
            auditLog.setCostTime(costTime);
            auditLog.setResultCode("0");
            auditLog.setResultMsg(annotation.description());
            auditLog.setUpdateTime(LocalDateTime.now());
            safeInsert(auditLog);
            return result;
        } catch (Throwable e) {
            long costTime = System.currentTimeMillis() - startTime;
            auditLog.setCostTime(costTime);
            auditLog.setResultCode(e.getClass().getSimpleName());
            auditLog.setResultMsg(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            auditLog.setUpdateTime(LocalDateTime.now());
            safeInsert(auditLog);
            throw e;
        }
    }

    /** 安全写入，失败不抛异常 */
    private void safeInsert(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    /** 获取注解 */
    private AuditLogRecord getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(AuditLogRecord.class);
    }

    /** 获取客户端真实IP */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /** 获取或生成 traceId */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 构建请求参数摘要 */
    private String buildParamSummary(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return null;
        return Stream.of(args)
                .filter(Objects::nonNull)
                .filter(a -> !(a instanceof HttpServletRequest))
                .filter(a -> !(a instanceof MultipartFile))
                .map(this::summarizeArg)
                .collect(Collectors.joining(", "));
    }

    /** 参数摘要：过滤敏感字段，截断 */
    private String summarizeArg(Object arg) {
        if (arg instanceof LoginUser) return "LoginUser(***)";
        String str = arg.toString();
        for (String field : SENSITIVE_FIELDS) {
            str = str.replaceAll("(?i)" + field + "=[^,&\\)]*", field + "=***");
        }
        if (str.length() > MAX_PARAM_LENGTH) {
            str = str.substring(0, MAX_PARAM_LENGTH) + "...";
        }
        return str;
    }
}
