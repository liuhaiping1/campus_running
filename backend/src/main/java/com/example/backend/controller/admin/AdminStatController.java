package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.service.AdminStatService;
import com.example.backend.vo.AdminOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员统计控制器
 * <p>
 * 提供管理端首页统计概览接口，需要ADMIN角色才能访问。
 * 路由受 {@code /api/admin/**} 的 SecurityConfig 保护。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/stat")
public class AdminStatController {

    private final AdminStatService adminStatService;

    /**
     * 构造函数注入统计服务
     *
     * @param adminStatService 统计服务
     */
    public AdminStatController(AdminStatService adminStatService) {
        this.adminStatService = adminStatService;
    }

    /**
     * 查询后台统计概览
     * <p>
     * 返回订单总数、今日订单数、已支付金额、退款金额、
     * 有效跑腿员数、待审核认证数、待处理申诉数和待处理退款数。
     * 仅ADMIN角色可访问。
     * </p>
     *
     * @return 统计概览数据
     */
    @GetMapping("/overview")
    public Result<AdminOverviewVO> overview() {
        AdminOverviewVO vo = adminStatService.getOverview();
        return Result.success(vo);
    }
}
