package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.annotation.AuditLogRecord;
import com.example.backend.common.Result;
import com.example.backend.dto.request.AdminUserStatusRequest;
import com.example.backend.dto.request.AdminUserUpdateRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.AdminUserService;
import com.example.backend.vo.AdminUserVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理控制器
 * <p>
 * 提供管理端用户列表查询、详情查看、资料修改和状态变更接口。
 * 所有接口需要ADMIN角色，由 SecurityConfig 的 /api/admin/** 规则保护。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 分页查询用户列表
     *
     * @param keyword    关键词（模糊匹配username/realName/nickName/phone），可选
     * @param userStatus 用户状态（1正常/2禁用），可选
     * @param roleCode   角色编码（STUDENT/RUNNER/ADMIN），可选
     * @param pageNum    页码，默认1
     * @param pageSize   每页大小，默认10
     * @return 用户分页结果
     */
    @GetMapping("/list")
    public Result<IPage<AdminUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userStatus,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<AdminUserVO> result = adminUserService.list(keyword, userStatus, roleCode,
                pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@PathVariable Long id) {
        AdminUserVO vo = adminUserService.detail(id);
        return Result.success(vo);
    }

    /**
     * 管理员修改用户资料
     * <p>
     * 允许修改：realName、nickName、phone、avatarUrl、gender。
     * 禁止修改：username、password、userStatus、roles。
     * </p>
     *
     * @param id      用户ID
     * @param request 修改请求
     * @return 修改后的用户详情
     */
    @AuditLogRecord(module = "USER", action = "UPDATE", bizType = "USER", description = "管理员修改用户资料")
    @PutMapping("/{id}")
    public Result<AdminUserVO> update(@PathVariable Long id,
                                      @Valid @RequestBody AdminUserUpdateRequest request) {
        AdminUserVO vo = adminUserService.update(id, request);
        return Result.success("用户资料修改成功", vo);
    }

    /**
     * 启用/禁用用户账号
     *
     * @param id        用户ID
     * @param request   状态变更请求
     * @param loginUser 当前登录管理员
     * @return 变更后的用户详情
     */
    @AuditLogRecord(module = "USER", action = "STATUS", bizType = "USER", description = "变更用户状态")
    @PutMapping("/{id}/status")
    public Result<AdminUserVO> updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody AdminUserStatusRequest request,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        AdminUserVO vo = adminUserService.updateStatus(id, request, loginUser.getUserId());
        return Result.success("用户状态更新成功", vo);
    }
}
