package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告保存请求DTO
 * <p>
 * 用于管理端新增和修改系统公告，包含标题、内容、类型和状态字段。
 * 新增时 title 和 content 由 {@code @Valid} 校验必填；
 * noticeType 和 noticeStatus 为可选字段，有默认值。
 * </p>
 */
@Data
public class NoticeSaveRequest {

    /** 公告标题，必填，最大100字 */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题最长100字")
    private String noticeTitle;

    /** 公告内容，必填 */
    @NotBlank(message = "公告内容不能为空")
    private String noticeContent;

    /** 公告类型：1普通 2重要 3维护，默认1 */
    private Integer noticeType;

    /** 公告状态：0草稿 1已发布 2已下架，默认0 */
    private Integer noticeStatus;
}
