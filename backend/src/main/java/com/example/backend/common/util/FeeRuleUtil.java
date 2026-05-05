package com.example.backend.common.util;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * 费用规则计算工具类
 */
public class FeeRuleUtil {

    private FeeRuleUtil() {}

    /**
     * 根据距离收费规则 JSON 计算距离费用
     * <p>
     * 规则格式为 JSON 数组：[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},...]
     * max=null 表示无上限。
     * </p>
     *
     * @param objectMapper JSON 解析器
     * @param ruleJson     收费规则 JSON 字符串
     * @param distanceKm   距离，单位 km
     * @return 距离费用
     */
    public static BigDecimal calculateDistanceFee(ObjectMapper objectMapper,
                                                  String ruleJson, BigDecimal distanceKm) {
        if (ruleJson == null || ruleJson.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "分类距离收费规则未配置");
        }

        try {
            JsonNode rules = objectMapper.readTree(ruleJson);
            if (!rules.isArray() || rules.size() == 0) {
                throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "距离收费规则格式错误");
            }

            if (distanceKm == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "预估距离不能为空");
            }

            for (JsonNode rule : rules) {
                JsonNode minNode = rule.get("min");
                JsonNode maxNode = rule.get("max");
                JsonNode feeNode = rule.get("fee");

                double min = minNode != null ? minNode.asDouble() : 0;
                Double max = (maxNode != null && !maxNode.isNull()) ? maxNode.asDouble() : null;
                double distanceValue = distanceKm.doubleValue();

                if (distanceValue >= min && (max == null || distanceValue < max)) {
                    return feeNode != null ? BigDecimal.valueOf(feeNode.asDouble()) : BigDecimal.ZERO;
                }
            }

            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE,
                    "距离" + distanceKm + "km未匹配到收费规则");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "距离收费规则解析失败");
        }
    }
}
