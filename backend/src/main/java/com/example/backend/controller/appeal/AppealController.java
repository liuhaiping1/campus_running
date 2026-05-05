package com.example.backend.controller.appeal;

import com.example.backend.annotation.AuditLogRecord;
import com.example.backend.common.Result;
import com.example.backend.dto.request.AppealSubmitRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.AppealService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user appeal endpoints.
 */
@RestController
@RequestMapping("/api/appeal")
public class AppealController {

    /**
     * Appeal service.
     */
    private final AppealService appealService;

    /**
     * Creates a controller with appeal service dependency.
     *
     * @param appealService appeal service
     */
    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    /**
     * Submits an appeal for the current user.
     *
     * @param loginUser current authenticated user
     * @param request appeal submit request
     * @return created appeal ID
     */
    @AuditLogRecord(module = "APPEAL", action = "SUBMIT", bizType = "APPEAL", description = "提交申诉")
    @PostMapping
    public Result<Long> submit(@AuthenticationPrincipal LoginUser loginUser,
                               @Valid @RequestBody AppealSubmitRequest request) {
        Long appealId = appealService.submit(loginUser.getUserId(), request);
        return Result.success(appealId);
    }
}
