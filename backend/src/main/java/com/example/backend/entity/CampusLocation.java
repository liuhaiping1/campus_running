package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园常用地点实体类
 */
@Data
@TableName("campus_location")
public class CampusLocation implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 地点名称
     */
    private String locationName;

    /**
     * 高德POI ID
     */
    private String amapPoiId;

    /**
     * 高德行政区划编码
     */
    private String amapAdcode;

    /**
     * 高德城市编码
     */
    private String amapCityCode;

    /**
     * 地点类型：1快递点 2外卖取餐点 3商超 4教学楼 5宿舍楼 6打印店 7其他
     */
    private Integer locationType;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 楼栋/区域名称
     */
    private String buildingName;

    /**
     * 详细地址
     */
    private String detailAddress;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 坐标系：GCJ02/WGS84/BD09
     */
    private String coordType;

    /**
     * 地点状态：1启用 2停用
     */
    private Integer locationStatus;

    /**
     * 排序
     */
    private Integer sortNo;

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
