package com.example.backend.vo;

import com.example.backend.entity.StationMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息响应视图对象
 */
@Data
@Builder
public class MessageVO {
    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    private String content;
    private Integer messageLevel;
    private Integer isRead;
    private LocalDateTime sendTime;
    /** 消息跳转地址 */
    private String jumpUrl;

    /**
     * 根据站内消息实体构建响应对象
     *
     * @param message 站内消息实体
     * @return 站内消息响应对象
     */
    public static MessageVO from(StationMessage message) {
        return MessageVO.builder()
                .id(message.getId())
                .bizType(message.getBizType())
                .bizId(message.getBizId())
                .title(message.getTitle())
                .content(message.getContent())
                .messageLevel(message.getMessageLevel())
                .isRead(message.getIsRead())
                .sendTime(message.getSendTime())
                .jumpUrl(message.getJumpUrl())
                .build();
    }
}
