package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.CategorySaveRequest;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.service.AdminCategoryService;
import com.example.backend.vo.CategoryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员任务分类服务实现类
 * <p>
 * 提供管理端任务分类的增删改查实现。
 * 新增和修改时校验名称/编码唯一性和费用规则JSON合法性；
 * 删除使用 MyBatis-Plus 逻辑删除。
 * 与前台 /api/category/list 互不干扰——管理端可访问停用分类，前台只返回启用分类。
 * </p>
 */
@Service
public class AdminCategoryServiceImpl implements AdminCategoryService {

    /** 启用状态 */
    private static final int ENABLED = 1;
    /** 默认收费规则版本 */
    private static final String DEFAULT_FEE_RULE_VERSION = "v1";

    private final ErrandCategoryMapper errandCategoryMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入Mapper和JSON处理器
     *
     * @param errandCategoryMapper 任务分类Mapper
     * @param objectMapper         Jackson JSON处理器
     */
    public AdminCategoryServiceImpl(ErrandCategoryMapper errandCategoryMapper, ObjectMapper objectMapper) {
        this.errandCategoryMapper = errandCategoryMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询任务分类列表（管理端）
     * <p>
     * 管理端不限制分类状态，可同时查看启用和停用分类。
     * 状态筛选和关键词搜索可选，结果按 sortNo 升序、createTime 倒序。
     * </p>
     *
     * @param categoryStatus 分类状态筛选，可选
     * @param keyword        关键词（模糊匹配分类名或编码），可选
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页分类列表
     */
    @Override
    public IPage<CategoryVO> list(Integer categoryStatus, String keyword, int pageNum, int pageSize) {
        Page<ErrandCategory> pageParam = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ErrandCategory> wrapper = new LambdaQueryWrapper<>();
        // 按状态筛选：1启用 2停用
        if (categoryStatus != null) {
            wrapper.eq(ErrandCategory::getCategoryStatus, categoryStatus);
        }
        // 关键词模糊匹配分类名称或编码
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> w.like(ErrandCategory::getCategoryName, trimmedKeyword)
                    .or()
                    .like(ErrandCategory::getCategoryCode, trimmedKeyword));
        }
        // 按排序号升序、创建时间倒序
        wrapper.orderByAsc(ErrandCategory::getSortNo)
                .orderByDesc(ErrandCategory::getCreateTime);

        Page<ErrandCategory> resultPage = errandCategoryMapper.selectPage(pageParam, wrapper);

        // 转换为 CategoryVO 列表
        List<CategoryVO> voList = new ArrayList<>();
        for (ErrandCategory category : resultPage.getRecords()) {
            voList.add(CategoryVO.from(category, objectMapper));
        }

        Page<CategoryVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 新增任务分类
     * <p>
     * 先校验名称和编码唯一性，再校验费用规则JSON，最后插入。
     * 可选字段设置默认值：urgentFee→0、feeRuleVersion→v1、sortNo→0、categoryStatus→1。
     * </p>
     *
     * @param request 分类保存请求
     * @return 新增后的分类视图对象
     */
    @Override
    public CategoryVO create(CategorySaveRequest request) {
        // 校验名称和编码唯一性
        validateNameUnique(request.getCategoryName(), null);
        validateCodeUnique(request.getCategoryCode(), null);

        // 校验费用规则JSON：距离和重量规则需包含 min/fee 字段
        validateFeeRuleJson(request.getDistanceFeeRule());
        if (request.getWeightFeeRule() != null && !request.getWeightFeeRule().trim().isEmpty()) {
            validateFeeRuleJson(request.getWeightFeeRule());
        }
        // 时段规则结构不同（code/start/end/fee），使用专门的校验方法
        if (request.getTimeFeeRule() != null && !request.getTimeFeeRule().trim().isEmpty()) {
            validateTimeFeeRuleJson(request.getTimeFeeRule());
        }

        ErrandCategory category = new ErrandCategory();
        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setBaseFee(request.getBaseFee());
        category.setDistanceFeeRule(request.getDistanceFeeRule());
        // 可选字段设置默认值
        category.setUrgentFee(request.getUrgentFee() != null ? request.getUrgentFee() : BigDecimal.ZERO);
        category.setWeightFeeRule(request.getWeightFeeRule());
        category.setTimeFeeRule(request.getTimeFeeRule());
        category.setFeeRuleVersion(
                request.getFeeRuleVersion() != null ? request.getFeeRuleVersion() : DEFAULT_FEE_RULE_VERSION);
        category.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
        category.setCategoryStatus(request.getCategoryStatus() != null ? request.getCategoryStatus() : ENABLED);

        errandCategoryMapper.insert(category);
        return CategoryVO.from(category, objectMapper);
    }

    /**
     * 修改任务分类
     * <p>
     * 校验分类存在性→名称唯一性（排除自身）→编码唯一性（排除自身）→费用规则JSON→执行更新。
     * </p>
     *
     * @param id      分类ID
     * @param request 分类保存请求
     * @return 修改后的分类视图对象
     */
    @Override
    public CategoryVO update(Long id, CategorySaveRequest request) {
        // 校验分类存在
        ErrandCategory category = errandCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 校验名称和编码唯一性（排除自身）
        validateNameUnique(request.getCategoryName(), id);
        validateCodeUnique(request.getCategoryCode(), id);

        // 校验费用规则JSON：距离和重量规则需包含 min/fee 字段
        validateFeeRuleJson(request.getDistanceFeeRule());
        if (request.getWeightFeeRule() != null && !request.getWeightFeeRule().trim().isEmpty()) {
            validateFeeRuleJson(request.getWeightFeeRule());
        }
        // 时段规则结构不同（code/start/end/fee），使用专门的校验方法
        if (request.getTimeFeeRule() != null && !request.getTimeFeeRule().trim().isEmpty()) {
            validateTimeFeeRuleJson(request.getTimeFeeRule());
        }

        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setBaseFee(request.getBaseFee());
        category.setDistanceFeeRule(request.getDistanceFeeRule());
        category.setUrgentFee(request.getUrgentFee() != null ? request.getUrgentFee() : BigDecimal.ZERO);
        category.setWeightFeeRule(request.getWeightFeeRule());
        category.setTimeFeeRule(request.getTimeFeeRule());
        category.setFeeRuleVersion(
                request.getFeeRuleVersion() != null ? request.getFeeRuleVersion() : DEFAULT_FEE_RULE_VERSION);
        category.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
        category.setCategoryStatus(request.getCategoryStatus() != null ? request.getCategoryStatus() : ENABLED);

        errandCategoryMapper.updateById(category);
        return CategoryVO.from(category, objectMapper);
    }

    /**
     * 逻辑删除任务分类
     * <p>
     * MyBatis-Plus 的 deleteById 会通过 {@code @TableLogic} 将 isDeleted 置为1。
     * 删除前校验分类是否存在。不做级联删除。
     * </p>
     *
     * @param id 分类ID
     */
    @Override
    public void delete(Long id) {
        // 校验分类存在
        ErrandCategory category = errandCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // MyBatis-Plus @TableLogic 会将此操作转为逻辑删除
        errandCategoryMapper.deleteById(id);
    }

    /**
     * 校验分类名称唯一性
     * <p>
     * 查询是否存在同名分类。修改时通过 excludeId 排除自身。
     * </p>
     *
     * @param name      分类名称
     * @param excludeId 修改时排除的ID（新增时传null）
     */
    private void validateNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<ErrandCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandCategory::getCategoryName, name);
        if (excludeId != null) {
            // 修改时排除自身
            wrapper.ne(ErrandCategory::getId, excludeId);
        }
        if (errandCategoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
    }

    /**
     * 校验分类编码唯一性
     * <p>
     * 查询是否存在同编码分类。修改时通过 excludeId 排除自身。
     * </p>
     *
     * @param code      分类编码
     * @param excludeId 修改时排除的ID（新增时传null）
     */
    private void validateCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<ErrandCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandCategory::getCategoryCode, code);
        if (excludeId != null) {
            // 修改时排除自身
            wrapper.ne(ErrandCategory::getId, excludeId);
        }
        if (errandCategoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_CODE_EXISTS);
        }
    }

    /**
     * 校验费用规则JSON合法性
     * <p>
     * 校验规则：
     * <ul>
     *   <li>必须是合法JSON数组</li>
     *   <li>数组不能为空</li>
     *   <li>每个元素必须包含 min 和 fee 字段</li>
     *   <li>max 可以为 null</li>
     * </ul>
     * 注意：本阶段不做区间连续性校验，后续可增强为连续区间校验。
     * </p>
     *
     * @param ruleJson 费用规则JSON字符串
     * @throws BusinessException JSON格式非法时抛出
     */
    private void validateFeeRuleJson(String ruleJson) {
        if (ruleJson == null || ruleJson.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "收费规则不能为空");
        }
        JsonNode rules;
        try {
            rules = objectMapper.readTree(ruleJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "收费规则JSON格式非法");
        }
        // 必须是数组
        if (!rules.isArray()) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "收费规则必须是JSON数组");
        }
        // 数组不能为空
        if (rules.size() == 0) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "收费规则数组不能为空");
        }
        // 每个元素需包含 min 和 fee 字段
        for (int i = 0; i < rules.size(); i++) {
            JsonNode rule = rules.get(i);
            if (!rule.has("min")) {
                throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE,
                        "收费规则第" + (i + 1) + "项缺少 min 字段");
            }
            if (!rule.has("fee")) {
                throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE,
                        "收费规则第" + (i + 1) + "项缺少 fee 字段");
            }
        }
        // 后续可增强：校验区间连续性、max=null 兜底、区间不重叠等
    }

    /**
     * 校验时段收费规则JSON合法性
     * <p>
     * 时段规则的结构为 {@code code/start/end/fee}，与距离和重量规则的 {@code min/max/fee} 结构不同。
     * 校验规则：
     * <ul>
     *   <li>必须是合法JSON数组</li>
     *   <li>数组不能为空</li>
     *   <li>每个元素必须包含 fee 字段</li>
     * </ul>
     * 注意：本阶段不做时段连续性和时间重叠校验，后续可增强。
     * </p>
     *
     * @param ruleJson 时段收费规则JSON字符串
     * @throws BusinessException JSON格式非法时抛出
     */
    private void validateTimeFeeRuleJson(String ruleJson) {
        if (ruleJson == null || ruleJson.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "时段收费规则不能为空");
        }
        JsonNode rules;
        try {
            rules = objectMapper.readTree(ruleJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "时段收费规则JSON格式非法");
        }
        // 必须是数组
        if (!rules.isArray()) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "时段收费规则必须是JSON数组");
        }
        // 数组不能为空
        if (rules.size() == 0) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE, "时段收费规则数组不能为空");
        }
        // 时段规则每个元素只需包含 fee 字段即可，不强制 min 字段
        for (int i = 0; i < rules.size(); i++) {
            JsonNode rule = rules.get(i);
            if (!rule.has("fee")) {
                throw new BusinessException(ErrorCode.CATEGORY_INVALID_FEE_RULE,
                        "时段收费规则第" + (i + 1) + "项缺少 fee 字段");
            }
        }
        // 后续可增强：校时段连续性、时间区间不重叠等
    }
}
