package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿订单分类扩展详情实体类
 * <p>
 * 按分类编码(category_code)垂直拆分不同跑腿场景的专属字段，
 * 避免 errand_order 表堆积大量 NULL 列。
 * </p>
 */
@Data
@TableName("errand_order_detail")
public class ErrandOrderDetail implements Serializable {

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
     * 订单编号冗余
     */
    private String orderNo;

    /**
     * 分类编码快照：EXPRESS_PICKUP/TAKEAWAY_PICKUP/SHOPPING/DOCUMENT_DELIVERY/TEMP_HELP
     */
    private String categoryCode;

    // ========== 代取快递专属字段 ==========

    /**
     * 快递公司
     */
    private String expressCompany;

    /**
     * 快递驿站或取件点名称
     */
    private String expressStation;

    /**
     * 快递单号
     */
    private String expressNo;

    /**
     * 快递取件码
     */
    private String expressPickupCode;

    /**
     * 取件手机号后四位
     */
    private String expressPhoneSuffix;

    /**
     * 包裹数量
     */
    private Integer packageCount;

    /**
     * 包裹重量，单位kg
     */
    private BigDecimal packageWeightKg;

    /**
     * 包裹大小：1小件 2中件 3大件
     */
    private Integer packageSize;

    // ========== 代取外卖专属字段 ==========

    /**
     * 外卖平台：MEITUAN/ELEME/OTHER
     */
    private String takeawayPlatform;

    /**
     * 外卖订单号
     */
    private String takeawayOrderNo;

    /**
     * 外卖取餐码
     */
    private String takeawayPickupCode;

    /**
     * 外卖绑定手机号后四位
     */
    private String takeawayPhoneSuffix;

    /**
     * 商家名称
     */
    private String merchantName;

    /**
     * 商家联系电话
     */
    private String merchantPhone;

    /**
     * 餐品数量
     */
    private Integer foodItemCount;

    /**
     * 预计可取时间
     */
    private LocalDateTime expectedPickupTime;

    /**
     * 是否需要保温：0否 1是
     */
    private Integer needInsulation;

    // ========== 代买商品专属字段 ==========

    /**
     * 代买商品清单快照（JSON字符串）
     */
    private String shoppingItems;

    /**
     * 代买预算金额
     */
    private BigDecimal shoppingBudget;

    /**
     * 是否允许价格浮动：0否 1是
     */
    private Integer allowPriceAdjust;

    // ========== 代送资料专属字段 ==========

    /**
     * 资料名称
     */
    private String documentName;

    /**
     * 资料数量
     */
    private Integer documentCount;

    /**
     * 资料交接说明
     */
    private String documentRemark;

    // ========== 临时帮办专属字段 ==========

    /**
     * 帮办类型
     */
    private String helpType;

    /**
     * 帮办详细内容
     */
    private String helpContent;

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
