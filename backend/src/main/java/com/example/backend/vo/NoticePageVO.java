package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 系统公告分页响应视图对象
 */
@Data
@Builder
public class NoticePageVO {
    private Long total;
    private List<NoticeVO> records;
}
