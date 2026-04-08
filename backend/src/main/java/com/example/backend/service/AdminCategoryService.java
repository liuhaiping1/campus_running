package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.dto.request.CategorySaveRequest;
import com.example.backend.vo.CategoryVO;

/**
 * 管理员任务分类服务接口
 * <p>
 * 提供管理端任务分类的增删改查能力，需要ADMIN角色。
 * 分类支持启用/停用状态，停用后前台的 /api/category/list 不可见。
 * </p>
 */
public interface AdminCategoryService {

    /**
     * 分页查询任务分类列表（管理端）
     * <p>
     * 与前台 {@code /api/category/list} 不同，管理端可查看启用和停用的分类。
     * 支持按分类状态和关键词（模糊匹配名称或编码）筛选。
     * 结果按 sortNo 升序、createTime 倒序排列。
     * </p>
     *
     * @param categoryStatus 分类状态筛选（1启用/2停用），可选
     * @param keyword        关键词（模糊匹配分类名或编码），可选
     * @param pageNum        页码，默认1
     * @param pageSize       每页大小，默认10
     * @return 分页分类列表
     */
    IPage<CategoryVO> list(Integer categoryStatus, String keyword, int pageNum, int pageSize);

    /**
     * 新增任务分类
     * <p>
     * 校验分类名称和编码唯一性、费用规则JSON合法性后新增。
     * </p>
     *
     * @param request 分类保存请求
     * @return 新增后的分类视图对象
     */
    CategoryVO create(CategorySaveRequest request);

    /**
     * 修改任务分类
     * <p>
     * 校验分类存在性、名称和编码不与其他分类冲突、费用规则JSON合法性后修改。
     * </p>
     *
     * @param id      分类ID
     * @param request 分类保存请求
     * @return 修改后的分类视图对象
     */
    CategoryVO update(Long id, CategorySaveRequest request);

    /**
     * 逻辑删除任务分类
     * <p>
     * 使用 MyBatis-Plus deleteById 执行逻辑删除，{@code @TableLogic} 自动处理。
     * 删除前校验分类是否存在。不做级联删除。
     * </p>
     *
     * @param id 分类ID
     */
    void delete(Long id);
}
