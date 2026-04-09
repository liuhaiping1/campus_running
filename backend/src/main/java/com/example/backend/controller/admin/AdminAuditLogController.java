package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.service.AdminAuditLogService;
import com.example.backend.vo.AuditLogVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理员审计日志查询控制器
 * <p>
 * 提供管理端审计日志的分页查询接口，需要ADMIN角色。
 * 路由受 {@code /api/admin/**} 的 SecurityConfig 保护。
 * 仅提供只读查询，不包含审计日志写入逻辑。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/audit-log")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    /**
     * 构造函数注入审计日志服务
     *
     * @param adminAuditLogService 审计日志服务
     */
    public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    /**
     * 分页查询审计日志
     * <p>
     * 支持按模块、动作、操作人ID、操作人角色、traceId 精确筛选，
     * 关键词模糊匹配请求路径/方法/IP/描述，以及创建时间范围筛选。
     * 结果按创建时间倒序排列。
     * </p>
     *
     * @param module         模块名称，可选
     * @param action         操作动作，可选
     * @param operatorUserId 操作人ID，可选
     * @param operatorRole   操作人角色，可选
     * @param traceId        链路追踪号，可选
     * @param keyword        关键词（模糊匹配请求URI/方法/IP/描述），可选
     * @param startTime      创建时间起始，可选
     * @param endTime        创建时间截止，可选
     * @param pageNum        页码，默认1
     * @param pageSize       每页大小，默认10
     * @return 分页审计日志列表
     */
    @GetMapping("/list")
    public Result<IPage<AuditLogVO>> list(@RequestParam(required = false) String module,
                                          @RequestParam(required = false) String action,
                                          @RequestParam(required = false) Long operatorUserId,
                                          @RequestParam(required = false) String operatorRole,
                                          @RequestParam(required = false) String traceId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                          @RequestParam(required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                          @RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        IPage<AuditLogVO> result = adminAuditLogService.list(
                module, action, operatorUserId, operatorRole, traceId,
                keyword, startTime, endTime, pageNum, pageSize);
        return Result.success(result);
    }
}
