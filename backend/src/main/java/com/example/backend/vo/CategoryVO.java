package com.example.backend.vo;

import com.example.backend.entity.ErrandCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 任务分类响应视图对象
 */
@Data
@Builder
public class CategoryVO {
    private Long id;
    private String categoryName;
    private String categoryCode;
    private BigDecimal baseFee;
    private JsonNode distanceFeeRule;
    private BigDecimal urgentFee;
    private JsonNode weightFeeRule;
    private JsonNode timeFeeRule;
    private String feeRuleVersion;
    private Integer sortNo;
    private Integer categoryStatus;

    /**
     * 根据任务分类实体构建响应对象
     *
     * @param category     任务分类实体
     * @param objectMapper JSON处理器
     * @return 任务分类响应对象
     */
    public static CategoryVO from(ErrandCategory category, ObjectMapper objectMapper) {
        return CategoryVO.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .baseFee(category.getBaseFee())
                .distanceFeeRule(readJson(category.getDistanceFeeRule(), objectMapper))
                .urgentFee(category.getUrgentFee())
                .weightFeeRule(readJson(category.getWeightFeeRule(), objectMapper))
                .timeFeeRule(readJson(category.getTimeFeeRule(), objectMapper))
                .feeRuleVersion(category.getFeeRuleVersion())
                .sortNo(category.getSortNo())
                .categoryStatus(category.getCategoryStatus())
                .build();
    }

    /**
     * 解析JSON格式的收费规则
     *
     * @param json         JSON字符串
     * @param objectMapper JSON处理器
     * @return JSON节点；解析失败时返回null
     */
    private static JsonNode readJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
