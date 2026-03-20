package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("appeal_record")
public class AppealRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String appealNo;

    private Long applyUserId;

    private String applyRole;

    private Integer appealType;

    private String appealContent;

    private String evidenceUrls;

    private Integer appealStatus;

    private Integer beforeOrderStatus;

    private Integer resultOrderStatus;

    private Integer responsibilityType;

    private Integer refundDecision;

    private Long handleAdminId;

    private String handleResult;

    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer isDeleted;
}
