package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.dto.request.NoticeSaveRequest;
import com.example.backend.dto.request.NoticeStatusRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.AdminNoticeService;
import com.example.backend.vo.NoticePageVO;
import com.example.backend.vo.NoticeVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员公告管理控制器
 * <p>
 * 提供管理端系统公告的增改查和状态变更接口，需要ADMIN角色。
 * 路由受 {@code /api/admin/**} 的 SecurityConfig 保护。
 * 与前台 {@code /api/notice/list} 互不干扰——管理端可访问草稿和下架公告。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/notice")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    /**
     * 构造函数注入公告管理服务
     *
     * @param adminNoticeService 公告管理服务
     */
    public AdminNoticeController(AdminNoticeService adminNoticeService) {
        this.adminNoticeService = adminNoticeService;
    }

    /**
     * 分页查询公告列表（管理端）
     * <p>
     * 可查看草稿、已发布和已下架的公告，支持按状态、类型和关键词筛选。
     * </p>
     *
     * @param noticeStatus 公告状态（0草稿/1已发布/2已下架），可选
     * @param noticeType   公告类型（1普通/2重要/3维护），可选
     * @param keyword      关键词（模糊匹配标题或内容），可选
     * @param pageNum      页码，默认1
     * @param pageSize     每页大小，默认10
     * @return 公告分页结果
     */
    @GetMapping("/list")
    public Result<NoticePageVO> list(@RequestParam(required = false) Integer noticeStatus,
                                     @RequestParam(required = false) Integer noticeType,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        NoticePageVO result = adminNoticeService.list(noticeStatus, noticeType, keyword, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 新增公告
     * <p>
     * 新增时若 status=1（已发布），自动设置 publishTime。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param request   公告保存请求
     * @return 新增后的公告
     */
    @PostMapping
    public Result<NoticeVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody NoticeSaveRequest request) {
        NoticeVO vo = adminNoticeService.create(loginUser.getUserId(), request);
        return Result.success("公告创建成功", vo);
    }

    /**
     * 修改公告
     * <p>
     * 支持修改标题、内容、类型和状态。
     * 状态变化时自动处理 publishTime 和 offlineTime。
     * </p>
     *
     * @param id      公告ID
     * @param request 公告保存请求
     * @return 修改后的公告
     */
    @PutMapping("/{id}")
    public Result<NoticeVO> update(@PathVariable Long id,
                                   @Valid @RequestBody NoticeSaveRequest request) {
        NoticeVO vo = adminNoticeService.update(id, request);
        return Result.success("公告修改成功", vo);
    }

    /**
     * 修改公告发布状态
     * <p>
     * 单独修改公告的发布状态（发布/下架/草稿），
     * 自动处理 publishTime 和 offlineTime。
     * </p>
     *
     * @param id      公告ID
     * @param request 状态变更请求
     * @return 变更后的公告
     */
    @PostMapping("/{id}/status")
    public Result<NoticeVO> updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody NoticeStatusRequest request) {
        NoticeVO vo = adminNoticeService.updateStatus(id, request);
        return Result.success("公告状态更新成功", vo);
    }
}
