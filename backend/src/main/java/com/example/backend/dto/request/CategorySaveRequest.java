package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 任务分类保存请求DTO
 * <p>
 * 用于管理端新增和修改任务分类，包含名称、编码、费用规则等字段。
 * 新增时所有必填字段由 {@code @Valid} 校验；修改时由 Service 层补全默认值。
 * </p>
 */
@Data
public class CategorySaveRequest {

    /** 分类名称，必填 */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    /** 分类编码，必填 */
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    /** 基础费用，必填 */
    @NotNull(message = "基础费用不能为空")
    @DecimalMin(value = "0", message = "基础费用不能为负数")
    private BigDecimal baseFee;

    /** 距离收费规则JSON，必填 */
    @NotBlank(message = "距离收费规则不能为空")
    private String distanceFeeRule;

    /** 加急附加费，默认0 */
    @DecimalMin(value = "0", message = "加急费不能为负数")
    private BigDecimal urgentFee;

    /** 重量收费规则JSON，可选 */
    private String weightFeeRule;

    /** 时段收费规则JSON，可选 */
    private String timeFeeRule;

    /** 收费规则版本，默认v1 */
    private String feeRuleVersion;

    /** 排序号，默认0 */
    private Integer sortNo;

    /** 分类状态：1启用 2停用，默认1 */
    private Integer categoryStatus;
}
