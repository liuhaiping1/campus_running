package com.example.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 站内消息分页响应视图对象
 */
@Data
@Builder
public class MessagePageVO {
    private Long total;
    private Long unreadCount;
    private List<MessageVO> records;
}
