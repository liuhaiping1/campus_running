package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 公告状态变更请求DTO
 * <p>
 * 用于管理端单独修改公告发布状态（发布/下架/草稿）。
 * </p>
 */
@Data
public class NoticeStatusRequest {

    /** 公告状态：0草稿 1已发布 2已下架 */
    @NotNull(message = "公告状态不能为空")
    private Integer noticeStatus;
}
