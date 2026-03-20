package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跑腿员认证实体类
 */
@Data
@TableName("runner_auth")
public class RunnerAuth implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 认证批次号
     */
    private String authBatchNo;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 证件类型：1学生证 2校园卡 3身份证(扩展)
     */
    private Integer certType;

    /**
     * 证件号码
     */
    private String certNo;

    /**
     * 证件正面图片
     */
    private String certFrontUrl;

    /**
     * 证件背面图片
     */
    private String certBackUrl;

    /**
     * 认证状态：0待审 1通过 2驳回 3失效
     */
    private Integer authStatus;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 审核管理员ID
     */
    private Long reviewAdminId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 认证失效时间
     */
    private LocalDateTime expireTime;

    /**
     * 当前标识：1当前提交记录 0历史记录
     */
    private Integer currentFlag;

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
