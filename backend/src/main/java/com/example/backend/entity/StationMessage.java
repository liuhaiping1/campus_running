package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息实体类
 */
@Data
@TableName("station_message")
public class StationMessage implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收人ID
     */
    private Long receiverUserId;

    /**
     * 业务类型：ORDER/AUTH/NOTICE/APPEAL等
     */
    private String bizType;

    /**
     * 业务主键
     */
    private Long bizId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息级别：1普通 2重要
     */
    private Integer messageLevel;

    /**
     * 是否已读：0否 1是
     */
    private Integer isRead;

    /**
     * 已读时间
     */
    private LocalDateTime readTime;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer isDeleted;
}
