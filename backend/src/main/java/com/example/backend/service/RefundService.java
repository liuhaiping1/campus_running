package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.dto.request.RefundApproveRequest;
import com.example.backend.vo.RefundRecordVO;

/**
 * Refund management service.
 */
public interface RefundService {

    /**
     * Lists refund records by page.
     *
     * @param page         page number starting from 1
     * @param size         page size
     * @param refundStatus optional refund status filter
     * @return paged refund records
     */
    IPage<RefundRecordVO> list(int page, int size, Integer refundStatus);

    /**
     * Approves a refund record.
     *
     * @param id      refund record id
     * @param adminId approval admin id
     * @param request approval request
     */
    void approve(Long id, Long adminId, RefundApproveRequest request);
}
