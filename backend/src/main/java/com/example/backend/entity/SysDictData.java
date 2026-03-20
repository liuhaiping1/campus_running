package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典数据实体类
 */
@Data
@TableName("sys_dict_data")
public class SysDictData implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 字典类型编码
     */
    private String dictType;

    /**
     * 字典值
     */
    private String dictValue;

    /**
     * 字典标签
     */
    private String dictLabel;

    /**
     * 排序
     */
    private Integer sortNo;

    /**
     * 前端样式类
     */
    private String cssClass;

    /**
     * 数据状态：1启用 2停用
     */
    private Integer dataStatus;

    /**
     * 备注
     */
    private String remark;

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
