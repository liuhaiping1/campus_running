package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.annotation.AuditLogRecord;
import com.example.backend.common.Result;
import com.example.backend.dto.request.AppealHandleRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.AppealService;
import com.example.backend.vo.AppealRecordVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for admin appeal management endpoints.
 */
@RestController
@RequestMapping("/api/admin/appeal")
public class AppealAdminController {

    /**
     * Appeal service.
     */
    private final AppealService appealService;

    /**
     * Creates a controller with appeal service dependency.
     *
     * @param appealService appeal service
     */
    public AppealAdminController(AppealService appealService) {
        this.appealService = appealService;
    }

    /**
     * Lists appeal records.
     *
     * @param page page number
     * @param size page size
     * @param appealStatus optional appeal status filter
     * @return paged appeal records
     */
    @GetMapping("/list")
    public Result<IPage<AppealRecordVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) Integer appealStatus) {
        return Result.success(appealService.list(page, size, appealStatus));
    }

    /**
     * Handles an appeal.
     *
     * @param id appeal ID
     * @param loginUser current admin user
     * @param request appeal handle request
     * @return operation result
     */
    @AuditLogRecord(module = "APPEAL", action = "HANDLE", bizType = "APPEAL", description = "处理申诉")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser,
                               @Valid @RequestBody AppealHandleRequest request) {
        appealService.handle(id, loginUser.getUserId(), request);
        return Result.success();
    }
}
