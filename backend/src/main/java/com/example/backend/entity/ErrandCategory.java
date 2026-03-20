package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务分类实体类
 */
@Data
@TableName("errand_category")
public class ErrandCategory implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 基础费用
     */
    private BigDecimal baseFee;

    /**
     * 距离收费规则（JSON）
     */
    private String distanceFeeRule;

    /**
     * 加急附加费
     */
    private BigDecimal urgentFee;

    /**
     * 重量收费规则（JSON）
     */
    private String weightFeeRule;

    /**
     * 时段收费规则（JSON）
     */
    private String timeFeeRule;

    /**
     * 收费规则版本
     */
    private String feeRuleVersion;

    /**
     * 排序
     */
    private Integer sortNo;

    /**
     * 分类状态：1启用 2停用
     */
    private Integer categoryStatus;

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
