package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.dto.request.AppealHandleRequest;
import com.example.backend.dto.request.AppealSubmitRequest;
import com.example.backend.vo.AppealRecordVO;

/**
 * Service interface for appeal submission and admin handling.
 */
public interface AppealService {

    /**
     * Submits an appeal for an order.
     *
     * @param userId current user ID
     * @param request appeal submit request
     * @return created appeal ID
     */
    Long submit(Long userId, AppealSubmitRequest request);

    /**
     * Lists appeal records for the admin console.
     *
     * @param page page number starting at 1
     * @param size page size
     * @param appealStatus optional appeal status filter
     * @return paged appeal records
     */
    IPage<AppealRecordVO> list(int page, int size, Integer appealStatus);

    /**
     * Handles an appeal in the admin console.
     *
     * @param id appeal ID
     * @param adminId current admin ID
     * @param request appeal handle request
     */
    void handle(Long id, Long adminId, AppealHandleRequest request);
}
