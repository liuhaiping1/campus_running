package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建请求DTO
 */
@Data
public class OrderCreateRequest {

    /** 任务分类ID */
    @NotNull(message = "任务分类不能为空")
    private Long categoryId;

    /** 任务标题 */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题最长100字")
    private String title;

    /** 任务描述 */
    @NotBlank(message = "任务描述不能为空")
    @Size(max = 500, message = "任务描述最长500字")
    private String orderDesc;

    /** 取件地址 */
    @NotBlank(message = "取件地址不能为空")
    private String pickupAddress;

    /** 送达地址 */
    @NotBlank(message = "送达地址不能为空")
    private String deliveryAddress;

    /** 取件点经度 */
    private BigDecimal pickupLng;

    /** 取件点纬度 */
    private BigDecimal pickupLat;

    /** 送达点经度 */
    private BigDecimal deliveryLng;

    /** 送达点纬度 */
    private BigDecimal deliveryLat;

    /** 预估距离（公里） */
    @NotNull(message = "预估距离不能为空")
    @DecimalMin(value = "0", message = "距离不能为负数")
    private BigDecimal distanceKm;

    /** 小费 */
    @DecimalMin(value = "0", message = "小费不能为负数")
    private BigDecimal tipFee;

    /** 期望完成时间 */
    @NotNull(message = "期望完成时间不能为空")
    @Future(message = "期望完成时间必须晚于当前时间")
    private LocalDateTime deadlineTime;

    // ========== 起点/取件点地址快照可选字段 ==========

    /** 起点联系人姓名 */
    private String pickupContactName;

    /** 起点联系人手机号 */
    private String pickupContactPhone;

    /** 起点校区名称 */
    private String pickupCampusName;

    /** 起点楼栋/区域名称 */
    private String pickupBuildingName;

    /** 起点地图服务标准化地址 */
    private String pickupFormattedAddress;

    /** 起点省份 */
    private String pickupProvinceName;

    /** 起点城市 */
    private String pickupCityName;

    /** 起点区县 */
    private String pickupDistrictName;

    /** 起点行政区划编码 */
    private String pickupAdcode;

    /** 起点地图POI ID */
    private String pickupMapPoiId;

    /** 起点地址来源：1手动填写 2用户地址簿 3校园地点库 */
    private Integer pickupAddressSource;

    /** 起点来源记录ID，如 user_address.id 或 campus_location.id */
    private Long pickupSourceRefId;

    // ========== 终点/送达点地址快照可选字段 ==========

    /** 终点联系人姓名 */
    private String deliveryContactName;

    /** 终点联系人手机号 */
    private String deliveryContactPhone;

    /** 终点校区名称 */
    private String deliveryCampusName;

    /** 终点楼栋/区域名称 */
    private String deliveryBuildingName;

    /** 终点地图服务标准化地址 */
    private String deliveryFormattedAddress;

    /** 终点省份 */
    private String deliveryProvinceName;

    /** 终点城市 */
    private String deliveryCityName;

    /** 终点区县 */
    private String deliveryDistrictName;

    /** 终点行政区划编码 */
    private String deliveryAdcode;

    /** 终点地图POI ID */
    private String deliveryMapPoiId;

    /** 终点地址来源：1手动填写 2用户地址簿 3校园地点库 */
    private Integer deliveryAddressSource;

    /** 终点来源记录ID，如 user_address.id 或 campus_location.id */
    private Long deliverySourceRefId;

    // ========== 快递代取可选字段 ==========

    /** 快递公司 */
    private String expressCompany;

    /** 快递驿站或取件点 */
    private String expressStation;

    /** 快递单号 */
    private String expressNo;

    /** 快递取件码 */
    private String expressPickupCode;

    /** 取件手机号后四位 */
    private String expressPhoneSuffix;

    /** 包裹数量 */
    private Integer packageCount;

    /** 包裹重量（kg） */
    private BigDecimal packageWeightKg;

    /** 包裹大小：1小件 2中件 3大件 */
    private Integer packageSize;

    // ========== 外卖代取可选字段 ==========

    /** 外卖平台：MEITUAN/ELEME/OTHER */
    private String takeawayPlatform;

    /** 外卖订单号 */
    private String takeawayOrderNo;

    /** 外卖取餐码 */
    private String takeawayPickupCode;

    /** 外卖绑定手机号后四位 */
    private String takeawayPhoneSuffix;

    /** 商家名称 */
    private String merchantName;

    /** 商家联系电话 */
    private String merchantPhone;

    /** 餐品数量 */
    private Integer foodItemCount;

    /** 预计可取时间 */
    private LocalDateTime expectedPickupTime;

    /** 是否需要保温：0否 1是 */
    private Integer needInsulation;

    // ========== 代买商品可选字段 ==========

    /** 代买商品清单快照（JSON字符串） */
    private String shoppingItems;

    /** 代买预算金额 */
    private BigDecimal shoppingBudget;

    /** 是否允许价格浮动：0否 1是 */
    private Integer allowPriceAdjust;

    // ========== 资料代送可选字段 ==========

    /** 资料名称 */
    private String documentName;

    /** 资料数量 */
    private Integer documentCount;

    /** 资料交接说明 */
    private String documentRemark;

    // ========== 临时帮办可选字段 ==========

    /** 帮办类型 */
    private String helpType;

    /** 帮办详细内容 */
    private String helpContent;

    // ========== 订单主表新增可选字段 ==========

    /** 订单附件地址，多个用逗号或JSON存储 */
    private String attachmentUrls;

    /** 订单联系人姓名 */
    private String contactName;

    /** 订单联系人手机号 */
    private String contactPhone;

    /** 路线策略：walking/bicycling/driving */
    private String routeStrategy;
}
