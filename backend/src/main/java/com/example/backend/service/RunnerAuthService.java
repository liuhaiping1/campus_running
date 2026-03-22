package com.example.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.backend.dto.request.RunnerAuthApplyRequest;
import com.example.backend.dto.request.RunnerAuthReviewRequest;
import com.example.backend.vo.RunnerAuthVO;

/**
 * 跑腿员认证服务接口
 * <p>
 * 定义跑腿员认证相关的业务操作方法，包括提交申请、审核认证和分页查询认证列表。
 * </p>
 */
public interface RunnerAuthService {

    /**
     * 提交跑腿员认证申请
     * <p>
     * 用户提交认证申请时，系统会先检查是否存在待审核或已通过的认证记录。
     * 如果存在待审核记录则不允许重复提交；如果已通过则无需再次申请。
     * 校验通过后，将历史认证记录标记为非当前，创建新的认证申请记录。
     * </p>
     *
     * @param userId  申请人用户ID
     * @param request 认证申请请求，包含学校名称、校区、证件类型、证件号码和证件图片
     */
    void apply(Long userId, RunnerAuthApplyRequest request);

    /**
     * 审核跑腿员认证申请
     * <p>
     * 管理员对待审核的认证申请进行审核，审核结果包括通过或驳回。
     * 驳回时需填写驳回原因；审核通过时，如果用户尚未拥有RUNNER角色，则自动授予该角色。
     * 只有状态为"待审核"的申请才能被审核。
     * </p>
     *
     * @param id       认证记录ID
     * @param adminId  审核管理员ID
     * @param request  审核请求，包含审核结果和驳回原因
     */
    void review(Long id, Long adminId, RunnerAuthReviewRequest request);

    /**
     * 分页查询跑腿员认证申请列表
     * <p>
     * 支持按认证状态筛选，结果按创建时间倒序排列。
     * 查询结果关联系统用户表，返回包含申请人真实姓名和手机号的完整视图对象。
     * </p>
     *
     * @param page       页码，从1开始
     * @param size       每页大小
     * @param authStatus 认证状态筛选，null表示查询全部状态
     * @return 分页认证视图对象列表
     */
    IPage<RunnerAuthVO> list(int page, int size, Integer authStatus);
}
