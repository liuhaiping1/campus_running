package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.common.Result;
import com.example.backend.dto.request.CategorySaveRequest;
import com.example.backend.service.AdminCategoryService;
import com.example.backend.vo.CategoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员任务分类管理控制器
 * <p>
 * 提供管理端任务分类的增删改查接口，需要ADMIN角色。
 * 路由受 {@code /api/admin/**} 的 SecurityConfig 保护。
 * 与前台 {@code /api/category/list} 互不干扰——管理端可访问停用的分类。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/category")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    /**
     * 构造函数注入分类管理服务
     *
     * @param adminCategoryService 分类管理服务
     */
    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    /**
     * 分页查询任务分类列表（管理端）
     * <p>
     * 可查看启用和停用的分类，支持按状态和关键词筛选。
     * </p>
     *
     * @param categoryStatus 分类状态（1启用/2停用），可选
     * @param keyword        关键词（模糊匹配名称或编码），可选
     * @param pageNum        页码，默认1
     * @param pageSize       每页大小，默认10
     * @return 分页分类列表
     */
    @GetMapping("/list")
    public Result<IPage<CategoryVO>> list(@RequestParam(required = false) Integer categoryStatus,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        IPage<CategoryVO> result = adminCategoryService.list(categoryStatus, keyword, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 新增任务分类
     * <p>
     * 校验名称/编码唯一性和费用规则JSON后新增。
     * </p>
     *
     * @param request 分类保存请求
     * @return 新增后的分类视图对象
     */
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategorySaveRequest request) {
        CategoryVO vo = adminCategoryService.create(request);
        return Result.success("分类创建成功", vo);
    }

    /**
     * 修改任务分类
     * <p>
     * 校验分类存在、名称/编码唯一性、费用规则JSON后修改。
     * </p>
     *
     * @param id      分类ID
     * @param request 分类保存请求
     * @return 修改后的分类视图对象
     */
    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id,
                                     @Valid @RequestBody CategorySaveRequest request) {
        CategoryVO vo = adminCategoryService.update(id, request);
        return Result.success("分类修改成功", vo);
    }

    /**
     * 逻辑删除任务分类
     * <p>
     * 分类存在时执行逻辑删除，不做级联删除。
     * </p>
     *
     * @param id 分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return Result.success("分类删除成功", null);
    }
}
