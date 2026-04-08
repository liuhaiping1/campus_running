package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.dto.request.RefundApproveRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.RefundService;
import com.example.backend.vo.RefundRecordVO;
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
 * Admin refund management controller.
 */
@RestController
@RequestMapping("/api/admin/refund")
public class RefundAdminController {

    private final RefundService refundService;

    /**
     * Creates a refund admin controller.
     *
     * @param refundService refund service
     */
    public RefundAdminController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * Lists refund records.
     *
     * @param page         page number
     * @param size         page size
     * @param refundStatus optional refund status filter
     * @return paged refund records
     */
    @GetMapping("/list")
    public Result<IPage<RefundRecordVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) Integer refundStatus) {
        return Result.success(refundService.list(page, size, refundStatus));
    }

    /**
     * Approves a refund record.
     *
     * @param id        refund record id
     * @param loginUser current admin user
     * @param request   approval request
     * @return approval result
     */
    @PostMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id,
                                  @AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody RefundApproveRequest request) {
        refundService.approve(id, loginUser.getUserId(), request);
        return Result.success("审核完成");
    }
}
