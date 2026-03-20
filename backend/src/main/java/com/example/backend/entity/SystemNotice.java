package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统公告实体类
 */
@Data
@TableName("system_notice")
public class SystemNotice implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 公告标题
     */
    private String noticeTitle;

    /**
     * 公告内容
     */
    private String noticeContent;

    /**
     * 公告类型：1普通 2重要 3维护
     */
    private Integer noticeType;

    /**
     * 公告状态：0草稿 1已发布 2已下架
     */
    private Integer noticeStatus;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 下架时间
     */
    private LocalDateTime offlineTime;

    /**
     * 发布人ID
     */
    private Long publisherId;

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
