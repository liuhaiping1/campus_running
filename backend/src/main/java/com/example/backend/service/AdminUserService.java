package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.dto.request.AdminUserStatusRequest;
import com.example.backend.dto.request.AdminUserUpdateRequest;
import com.example.backend.vo.AdminUserVO;

/**
 * 管理员用户管理服务接口
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表
     *
     * @param keyword  关键词（模糊匹配username/realName/nickName/phone）
     * @param userStatus 用户状态：1正常 2禁用
     * @param roleCode 角色编码：STUDENT/RUNNER/ADMIN
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 用户分页结果
     */
    IPage<AdminUserVO> list(String keyword, Integer userStatus, String roleCode,
                            int pageNum, int pageSize);

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    AdminUserVO detail(Long id);

    /**
     * 管理员修改用户资料
     *
     * @param id      用户ID
     * @param request 修改请求
     * @return 修改后的用户详情
     */
    AdminUserVO update(Long id, AdminUserUpdateRequest request);

    /**
     * 启用/禁用用户账号
     *
     * @param id         用户ID
     * @param request    状态变更请求
     * @param operatorId 当前操作管理员ID
     * @return 变更后的用户详情
     */
    AdminUserVO updateStatus(Long id, AdminUserStatusRequest request, Long operatorId);
}
