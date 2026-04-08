package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.NoticeStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.NoticeSaveRequest;
import com.example.backend.dto.request.NoticeStatusRequest;
import com.example.backend.entity.SystemNotice;
import com.example.backend.mapper.SystemNoticeMapper;
import com.example.backend.service.AdminNoticeService;
import com.example.backend.vo.NoticePageVO;
import com.example.backend.vo.NoticeVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 管理员公告服务实现类
 * <p>
 * 提供管理端系统公告的增改查和状态变更实现。
 * 与前台 /api/notice/list 不同，管理端能操作所有状态的公告，
 * 支持按状态和类型筛选、关键词模糊搜索。
 * 状态变更时自动处理 publishTime 和 offlineTime。
 * </p>
 */
@Service
public class AdminNoticeServiceImpl implements AdminNoticeService {

    /** 默认公告类型：普通 */
    private static final int DEFAULT_NOTICE_TYPE = 1;

    private final SystemNoticeMapper systemNoticeMapper;

    /**
     * 构造函数注入Mapper
     *
     * @param systemNoticeMapper 系统公告Mapper
     */
    public AdminNoticeServiceImpl(SystemNoticeMapper systemNoticeMapper) {
        this.systemNoticeMapper = systemNoticeMapper;
    }

    /**
     * 分页查询公告列表（管理端）
     * <p>
     * 管理端不限制公告状态，可查看草稿、已发布和已下架公告。
     * 支持按状态、类型和关键词筛选，结果按创建时间倒序。
     * </p>
     *
     * @param noticeStatus 公告状态筛选
     * @param noticeType   公告类型筛选
     * @param keyword      关键词模糊匹配
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @return 公告分页结果
     */
    @Override
    public NoticePageVO list(Integer noticeStatus, Integer noticeType, String keyword,
                              int pageNum, int pageSize) {
        Page<SystemNotice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<>();

        // 按公告状态筛选：0草稿 1已发布 2已下架
        if (noticeStatus != null) {
            wrapper.eq(SystemNotice::getNoticeStatus, noticeStatus);
        }
        // 按公告类型筛选
        if (noticeType != null) {
            wrapper.eq(SystemNotice::getNoticeType, noticeType);
        }
        // 关键词模糊匹配标题或内容
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> w.like(SystemNotice::getNoticeTitle, trimmedKeyword)
                    .or()
                    .like(SystemNotice::getNoticeContent, trimmedKeyword));
        }
        // 按创建时间倒序
        wrapper.orderByDesc(SystemNotice::getCreateTime);

        Page<SystemNotice> result = systemNoticeMapper.selectPage(page, wrapper);
        return NoticePageVO.builder()
                .total(result.getTotal())
                .records(result.getRecords().stream()
                        .map(NoticeVO::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 新增公告
     * <p>
     * 新增时若 noticeStatus 为已发布，则自动设置 publishTime 为当前时间；
     * 否则 publishTime 保持 null。publisherId 记录当前管理员ID。
     * </p>
     *
     * @param adminUserId 当前管理员用户ID
     * @param request     公告保存请求
     * @return 新增后的公告视图对象
     */
    @Override
    public NoticeVO create(Long adminUserId, NoticeSaveRequest request) {
        SystemNotice notice = new SystemNotice();
        notice.setNoticeTitle(request.getNoticeTitle());
        notice.setNoticeContent(request.getNoticeContent());
        // 默认公告类型为普通（1）
        int type = request.getNoticeType() != null ? request.getNoticeType() : DEFAULT_NOTICE_TYPE;
        // 默认状态为草稿（0）
        int status = request.getNoticeStatus() != null ? request.getNoticeStatus()
                : NoticeStatusEnum.DRAFT.getCode();
        // 校验类型和状态白名单
        validateNoticeType(type);
        validateNoticeStatus(status);

        notice.setNoticeType(type);
        notice.setNoticeStatus(status);
        notice.setPublisherId(adminUserId);

        // 如果直接发布，设置发布时间
        if (status == NoticeStatusEnum.PUBLISHED.getCode()) {
            notice.setPublishTime(LocalDateTime.now());
        }

        systemNoticeMapper.insert(notice);
        return NoticeVO.from(notice);
    }

    /**
     * 修改公告
     * <p>
     * 修改时根据新旧状态变化处理时间字段：
     * <ul>
     *   <li>从非发布状态改为已发布：设置 publishTime = 当前时间</li>
     *   <li>保持已发布状态：保留原 publishTime 不覆盖</li>
     *   <li>改为下架状态：设置 offlineTime = 当前时间</li>
     * </ul>
     * </p>
     *
     * @param id      公告ID
     * @param request 公告保存请求
     * @return 修改后的公告视图对象
     */
    @Override
    public NoticeVO update(Long id, NoticeSaveRequest request) {
        SystemNotice notice = systemNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }

        int oldStatus = notice.getNoticeStatus() != null ? notice.getNoticeStatus() : 0;
        int newStatus = request.getNoticeStatus() != null ? request.getNoticeStatus()
                : NoticeStatusEnum.DRAFT.getCode();
        int newType = request.getNoticeType() != null ? request.getNoticeType() : DEFAULT_NOTICE_TYPE;
        // 校验类型和状态白名单
        validateNoticeType(newType);
        validateNoticeStatus(newStatus);

        notice.setNoticeTitle(request.getNoticeTitle());
        notice.setNoticeContent(request.getNoticeContent());
        notice.setNoticeType(newType);
        notice.setNoticeStatus(newStatus);

        // 从非发布状态改为已发布时，设置发布时间
        if (oldStatus != NoticeStatusEnum.PUBLISHED.getCode()
                && newStatus == NoticeStatusEnum.PUBLISHED.getCode()) {
            notice.setPublishTime(LocalDateTime.now());
        }
        // 改为下架时，设置下架时间
        if (newStatus == NoticeStatusEnum.OFFLINE.getCode()) {
            notice.setOfflineTime(LocalDateTime.now());
        }

        systemNoticeMapper.updateById(notice);
        return NoticeVO.from(notice);
    }

    /**
     * 修改公告发布状态
     * <p>
     * 单独修改公告状态，适用于发布、下架、回退草稿操作。
     * 状态值校验：只允许0（草稿）、1（已发布）、2（已下架）。
     * </p>
     * <ul>
     *   <li>改为已发布：若 publishTime 为空则设置为当前时间</li>
     *   <li>改为已下架：设置 offlineTime = 当前时间</li>
     *   <li>改为草稿：不强制清空 publishTime / offlineTime</li>
     * </ul>
     *
     * @param id      公告ID
     * @param request 状态变更请求
     * @return 变更后的公告视图对象
     */
    @Override
    public NoticeVO updateStatus(Long id, NoticeStatusRequest request) {
        SystemNotice notice = systemNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }

        int newStatus = request.getNoticeStatus();

        // 校验状态值合法性
        validateNoticeStatus(newStatus);

        notice.setNoticeStatus(newStatus);

        // 改为已发布：若尚未设置发布时间则自动设置
        if (newStatus == NoticeStatusEnum.PUBLISHED.getCode()
                && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        // 改为已下架：设置下架时间
        if (newStatus == NoticeStatusEnum.OFFLINE.getCode()) {
            notice.setOfflineTime(LocalDateTime.now());
        }

        systemNoticeMapper.updateById(notice);
        return NoticeVO.from(notice);
    }

    /**
     * 校验公告状态白名单
     * <p>
     * 公告状态只允许 0（草稿）、1（已发布）、2（已下架）。
     * 此校验被 create、update、updateStatus 共用，防止脏数据落库。
     * </p>
     *
     * @param status 公告状态值
     * @throws BusinessException 状态值非法时抛出
     */
    private void validateNoticeStatus(int status) {
        if (NoticeStatusEnum.getByCode(status) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "公告状态值非法，仅支持0/1/2");
        }
    }

    /**
     * 校验公告类型白名单
     * <p>
     * 公告类型只允许 1（普通）、2（重要）、3（维护）。
     * 此校验被 create 和 update 共用，防止脏数据落库。
     * </p>
     *
     * @param type 公告类型值
     * @throws BusinessException 类型值非法时抛出
     */
    private void validateNoticeType(int type) {
        // 公告类型白名单：1普通 2重要 3维护
        if (type < 1 || type > 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "公告类型值非法，仅支持1/2/3（普通/重要/维护）");
        }
    }
}
