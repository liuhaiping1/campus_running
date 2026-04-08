package com.example.backend.controller.evaluation;

import com.example.backend.common.Result;
import com.example.backend.dto.request.EvaluationSubmitRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.EvaluationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for order evaluation endpoints.
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Submit an evaluation for an order.
     *
     * @param loginUser the current authenticated user
     * @param request the evaluation submission request
     * @return Result containing the evaluation ID
     */
    @PostMapping
    public Result<Long> submitEvaluation(
            @AuthenticationPrincipal LoginUser loginUser,
            @Validated @RequestBody EvaluationSubmitRequest request) {
        Long evaluationId = evaluationService.submit(loginUser.getUserId(), request);
        return Result.success(evaluationId);
    }
}
