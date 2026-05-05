package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单地址快照实体类
 * <p>
 * 下单时将取件点、送达点的完整地址信息保存为此表记录，
 * 坐标以此表为准，errand_order 中的经纬度为冗余快照。
 * </p>
 */
@Data
@TableName("errand_order_address")
public class ErrandOrderAddress implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 地址角色：1起点/取件点/购买点/商家 2终点/送达点 3帮办地点
     */
    private Integer addressRole;

    /**
     * 地址来源：1手动填写 2用户地址簿 3校园地点库
     */
    private Integer addressSource;

    /**
     * 地图服务商：1高德 2百度 3腾讯 9系统兜底
     */
    private Integer mapProvider;

    /**
     * 地图POI ID
     */
    private String mapPoiId;

    /**
     * 来源记录ID，如 user_address.id 或 campus_location.id
     */
    private Long sourceRefId;

    /**
     * 联系人姓名快照
     */
    private String contactName;

    /**
     * 联系人手机号快照
     */
    private String contactPhone;

    /**
     * 校区名称快照
     */
    private String campusName;

    /**
     * 楼栋/地点名称快照
     */
    private String buildingName;

    /**
     * 详细地址快照，订单地址权威字段
     */
    private String detailAddress;

    /**
     * 地图服务返回的标准化地址
     */
    private String formattedAddress;

    /**
     * 省份名称
     */
    private String provinceName;

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * 区县名称
     */
    private String districtName;

    /**
     * 行政区划编码
     */
    private String adcode;

    /**
     * 经度权威快照，用于距离计算
     */
    private BigDecimal longitude;

    /**
     * 纬度权威快照，用于距离计算
     */
    private BigDecimal latitude;

    /**
     * 坐标系：GCJ02/WGS84/BD09
     */
    private String coordType;

    /**
     * 地理编码状态：0未解析 1成功 2失败 3人工定位
     */
    private Integer geocodeStatus;

    /**
     * 地理编码时间
     */
    private LocalDateTime geocodeTime;

    /**
     * 地址备注
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
