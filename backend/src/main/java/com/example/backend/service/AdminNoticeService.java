package com.example.backend.service;

import com.example.backend.dto.request.NoticeSaveRequest;
import com.example.backend.dto.request.NoticeStatusRequest;
import com.example.backend.vo.NoticePageVO;
import com.example.backend.vo.NoticeVO;

/**
 * 管理员公告服务接口
 * <p>
 * 提供管理端系统公告的增改查和状态变更能力，需要ADMIN角色。
 * 与前台 /api/notice/list 不同，管理端可以查看草稿、已发布和已下架的公告。
 * </p>
 */
public interface AdminNoticeService {

    /**
     * 分页查询公告列表（管理端）
     * <p>
     * 管理端不限制公告状态，可查看草稿、已发布和已下架的公告。
     * 支持按状态、类型和关键词（模糊匹配标题或内容）筛选。
     * 结果按创建时间倒序排列。
     * </p>
     *
     * @param noticeStatus 公告状态筛选，可选
     * @param noticeType   公告类型筛选，可选
     * @param keyword      关键词（模糊匹配标题或内容），可选
     * @param pageNum      页码，默认1
     * @param pageSize     每页大小，默认10
     * @return 公告分页结果
     */
    NoticePageVO list(Integer noticeStatus, Integer noticeType, String keyword, int pageNum, int pageSize);

    /**
     * 新增公告
     * <p>
     * 新增时如果 noticeStatus = 已发布，则自动设置 publishTime 为当前时间。
     * 如果 noticeStatus 为草稿或下架，则不设置 publishTime。
     * </p>
     *
     * @param adminUserId 当前管理员用户ID
     * @param request     公告保存请求
     * @return 新增后的公告视图对象
     */
    NoticeVO create(Long adminUserId, NoticeSaveRequest request);

    /**
     * 修改公告
     * <p>
     * 修改时根据状态变化处理时间字段：
     * <ul>
     *   <li>从非发布改为已发布：设置 publishTime = 当前时间</li>
     *   <li>保持已发布：不覆盖原 publishTime</li>
     *   <li>改为下架：设置 offlineTime = 当前时间</li>
     * </ul>
     * </p>
     *
     * @param id      公告ID
     * @param request 公告保存请求
     * @return 修改后的公告视图对象
     */
    NoticeVO update(Long id, NoticeSaveRequest request);

    /**
     * 修改公告发布状态
     * <p>
     * 单独修改公告的发布状态（发布/下架/草稿）。
     * 发布时如果 publishTime 为空则自动设置；下架时设置 offlineTime。
     * </p>
     *
     * @param id      公告ID
     * @param request 状态变更请求
     * @return 变更后的公告视图对象
     */
    NoticeVO updateStatus(Long id, NoticeStatusRequest request);
}
