package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.NoticeStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.NoticeSaveRequest;
import com.example.backend.dto.request.NoticeStatusRequest;
import com.example.backend.entity.SystemNotice;
import com.example.backend.mapper.SystemNoticeMapper;
import com.example.backend.service.impl.AdminNoticeServiceImpl;
import com.example.backend.vo.NoticePageVO;
import com.example.backend.vo.NoticeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminNoticeService 单元测试类
 * <p>
 * 使用 Mockito 对 {@link AdminNoticeServiceImpl} 进行单元测试，
 * 覆盖管理端公告分页查询、新增、修改、状态变更四大功能。
 * Mock 所有 Mapper 依赖，隔离数据库。
 * </p>
 *
 * @author campus_running
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminNoticeService 单元测试")
class AdminNoticeServiceTest {

    /** Mock 公告 Mapper */
    @Mock
    private SystemNoticeMapper systemNoticeMapper;

    /** 自动注入 Mock 的被测对象 */
    @InjectMocks
    private AdminNoticeServiceImpl adminNoticeService;

    /** 测试管理员ID */
    private static final Long ADMIN_USER_ID = 1L;

    /** 测试用公告实体 */
    private SystemNotice draftNotice;
    private SystemNotice publishedNotice;

    /**
     * 每个测试方法执行前构建通用测试数据
     */
    @BeforeEach
    void setUp() {
        draftNotice = buildNotice(1L, "测试草稿公告", "草稿内容",
                NoticeStatusEnum.DRAFT.getCode(), null);
        publishedNotice = buildNotice(2L, "已发布公告", "已发布内容",
                NoticeStatusEnum.PUBLISHED.getCode(), LocalDateTime.now().minusHours(1));
    }

    // =========================================================================
    // 列表查询测试组
    // =========================================================================

    /**
     * 列表查询测试组
     */
    @Nested
    @DisplayName("公告列表查询")
    class ListTests {

        /**
         * 测试管理端无筛选分页返回所有公告
         * <p>
         * 管理端不限制状态，应返回草稿和已发布等所有状态的公告。
         * </p>
         */
        @Test
        @DisplayName("无筛选条件时应返回所有状态的公告")
        void shouldReturnAllNoticesWithoutFilter() {
            // Given: 一个草稿公告和一个已发布公告
            when(systemNoticeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<SystemNotice> page = inv.getArgument(0);
                        page.setRecords(Arrays.asList(draftNotice, publishedNotice));
                        page.setTotal(2L);
                        return page;
                    });

            // When: 管理员查询
            NoticePageVO result = adminNoticeService.list(null, null, null, 1, 10);

            // Then: 返回2条
            assertNotNull(result, "返回结果不应为null");
            assertEquals(2L, result.getTotal(), "总数应为2");
            assertEquals(2, result.getRecords().size(), "记录数应为2");
            assertEquals("测试草稿公告", result.getRecords().get(0).getNoticeTitle());
            assertEquals("已发布公告", result.getRecords().get(1).getNoticeTitle());
        }

        /**
         * 测试按公告状态筛选
         */
        @Test
        @DisplayName("应按公告状态筛选")
        void shouldFilterByNoticeStatus() {
            // Given: 仅草稿
            when(systemNoticeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<SystemNotice> page = inv.getArgument(0);
                        page.setRecords(Collections.singletonList(draftNotice));
                        page.setTotal(1L);
                        return page;
                    });

            // When: 筛选草稿状态
            NoticePageVO result = adminNoticeService.list(
                    NoticeStatusEnum.DRAFT.getCode(), null, null, 1, 10);

            // Then: 仅返回草稿
            assertEquals(1L, result.getTotal(), "筛选后总数应为1");
            assertEquals(Integer.valueOf(0), result.getRecords().get(0).getNoticeStatus(),
                    "公告状态应为草稿(0)");
        }

        /**
         * 测试按公告类型筛选
         */
        @Test
        @DisplayName("应按公告类型筛选")
        void shouldFilterByNoticeType() {
            // Given: 类型为2（重要）
            draftNotice.setNoticeType(2);
            when(systemNoticeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<SystemNotice> page = inv.getArgument(0);
                        page.setRecords(Collections.singletonList(draftNotice));
                        page.setTotal(1L);
                        return page;
                    });

            // When: 筛选重要类型
            NoticePageVO result = adminNoticeService.list(null, 2, null, 1, 10);

            // Then: 返回类型匹配的公告
            assertEquals(1L, result.getTotal(), "筛选后总数应为1");
            assertEquals(Integer.valueOf(2), result.getRecords().get(0).getNoticeType(),
                    "公告类型应为重要(2)");
        }

        /**
         * 测试按关键词模糊搜索
         */
        @Test
        @DisplayName("应按关键词模糊匹配标题或内容")
        void shouldFilterByKeyword() {
            // Given: 匹配关键词的公告
            when(systemNoticeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Page<SystemNotice> page = inv.getArgument(0);
                        page.setRecords(Collections.singletonList(publishedNotice));
                        page.setTotal(1L);
                        return page;
                    });

            // When: 搜索 "发布"
            NoticePageVO result = adminNoticeService.list(null, null, "发布", 1, 10);

            // Then: 返回匹配公告
            assertEquals(1L, result.getTotal(), "关键词搜索后总数应为1");
        }
    }

    // =========================================================================
    // 新增公告测试组
    // =========================================================================

    /**
     * 新增公告测试组
     */
    @Nested
    @DisplayName("新增公告")
    class CreateTests {

        /**
         * 测试新增草稿公告，publishTime 应为空
         */
        @Test
        @DisplayName("新增草稿公告时 publishTime 为空")
        void shouldCreateDraftNoticeWithoutPublishTime() {
            // Given
            doAnswer(inv -> {
                SystemNotice n = inv.getArgument(0);
                n.setId(10L);
                return 1;
            }).when(systemNoticeMapper).insert(any(SystemNotice.class));

            NoticeSaveRequest request = buildSaveRequest("新草稿公告", "草稿内容", 1, 0);

            // When: 新增草稿
            NoticeVO result = adminNoticeService.create(ADMIN_USER_ID, request);

            // Then: publishTime 为空
            assertNotNull(result, "返回结果不应为null");
            assertEquals("新草稿公告", result.getNoticeTitle(), "标题应一致");
            assertEquals(Integer.valueOf(0), result.getNoticeStatus(), "状态应为草稿(0)");
            assertNull(result.getPublishTime(), "草稿公告的publishTime应为null");

            // 验证 insert 内容
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).insert(captor.capture());
            SystemNotice inserted = captor.getValue();
            assertEquals(ADMIN_USER_ID, inserted.getPublisherId(), "发布人ID应为管理员ID");
            assertNull(inserted.getPublishTime(), "草稿公告publishTime应为null");
        }

        /**
         * 测试新增已发布公告，publishTime 不为空
         */
        @Test
        @DisplayName("新增已发布公告时 publishTime 应自动设置")
        void shouldCreatePublishedNoticeWithPublishTime() {
            // Given
            doAnswer(inv -> {
                SystemNotice n = inv.getArgument(0);
                n.setId(20L);
                return 1;
            }).when(systemNoticeMapper).insert(any(SystemNotice.class));

            NoticeSaveRequest request = buildSaveRequest("新发布公告", "发布内容", 1, 1);

            // When: 新增时直接发布
            NoticeVO result = adminNoticeService.create(ADMIN_USER_ID, request);

            // Then: publishTime 已设置
            assertNotNull(result, "返回结果不应为null");

            // 验证 insert 的 publishTime 不为空
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).insert(captor.capture());
            SystemNotice inserted = captor.getValue();
            assertNotNull(inserted.getPublishTime(), "已发布公告的publishTime不应为null");
        }

        /**
         * 测试新增时传非法状态值（如5）应抛出异常
         */
        @Test
        @DisplayName("新增时传非法状态应抛出异常")
        void shouldThrowWhenInvalidStatusOnCreate() {
            NoticeSaveRequest request = buildSaveRequest("测试", "内容", 1, 5);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.create(ADMIN_USER_ID, request),
                    "非法状态值应抛出 BusinessException");

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode(),
                    "错误码应为 BAD_REQUEST");
            assertTrue(exception.getMessage().contains("状态"),
                    "错误信息应提示状态值问题");

            verify(systemNoticeMapper, never()).insert(any(SystemNotice.class));
        }

        /**
         * 测试新增时传非法类型值（如99）应抛出异常
         */
        @Test
        @DisplayName("新增时传非法类型应抛出异常")
        void shouldThrowWhenInvalidTypeOnCreate() {
            NoticeSaveRequest request = buildSaveRequest("测试", "内容", 99, 0);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.create(ADMIN_USER_ID, request),
                    "非法类型值应抛出 BusinessException");

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode(),
                    "错误码应为 BAD_REQUEST");
            assertTrue(exception.getMessage().contains("类型"),
                    "错误信息应提示类型值问题");

            verify(systemNoticeMapper, never()).insert(any(SystemNotice.class));
        }
    }

    // =========================================================================
    // 修改公告测试组
    // =========================================================================

    /**
     * 修改公告测试组
     */
    @Nested
    @DisplayName("修改公告")
    class UpdateTests {

        /**
         * 测试修改不存在的公告
         */
        @Test
        @DisplayName("修改不存在公告应抛出 NOTICE_NOT_FOUND")
        void shouldThrowWhenNoticeNotFound() {
            // Given: 公告不存在
            when(systemNoticeMapper.selectById(999L)).thenReturn(null);

            NoticeSaveRequest request = buildSaveRequest("测试", "内容", 1, 0);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.update(999L, request),
                    "公告不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.NOTICE_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 NOTICE_NOT_FOUND");
            verify(systemNoticeMapper, never()).updateById(any(SystemNotice.class));
        }

        /**
         * 测试从草稿改为已发布时设置 publishTime
         */
        @Test
        @DisplayName("从草稿改为已发布时应设置 publishTime")
        void shouldSetPublishTimeWhenChangingToPublished() {
            // Given: 原公告为草稿，publishTime 为空
            when(systemNoticeMapper.selectById(1L)).thenReturn(draftNotice);
            when(systemNoticeMapper.updateById(any(SystemNotice.class))).thenReturn(1);

            NoticeSaveRequest request = buildSaveRequest("修改后标题", "修改后内容", 1, 1);

            // When: 修改为已发布
            adminNoticeService.update(1L, request);

            // Then: publishTime 被设置为当前时间
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).updateById(captor.capture());
            SystemNotice updated = captor.getValue();
            assertEquals(Integer.valueOf(1), updated.getNoticeStatus(), "状态应为已发布(1)");
            assertNotNull(updated.getPublishTime(), "从草稿改为发布时publishTime应被设置");
        }

        /**
         * 测试已发布公告再次修改时不覆盖原 publishTime
         * <p>
         * 保持已发布状态时，原 publishTime 不应被覆盖。
         * </p>
         */
        @Test
        @DisplayName("已发布公告再次修改时应保留原 publishTime")
        void shouldPreservePublishTimeWhenAlreadyPublished() {
            // Given: 公告已发布，有原始 publishTime
            LocalDateTime originalPublishTime = LocalDateTime.now().minusDays(1);
            publishedNotice.setPublishTime(originalPublishTime);
            when(systemNoticeMapper.selectById(2L)).thenReturn(publishedNotice);
            when(systemNoticeMapper.updateById(any(SystemNotice.class))).thenReturn(1);

            NoticeSaveRequest request = buildSaveRequest("修改标题", "修改内容", 1, 1);

            // When: 修改已发布公告（保持发布状态）
            adminNoticeService.update(2L, request);

            // Then: publishTime 保持不变
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).updateById(captor.capture());
            SystemNotice updated = captor.getValue();
            assertEquals(originalPublishTime, updated.getPublishTime(),
                    "已发布公告保持发布状态时原publishTime不变");
        }

        /**
         * 测试修改为已下架时设置 offlineTime
         */
        @Test
        @DisplayName("修改为下架时应设置 offlineTime")
        void shouldSetOfflineTimeWhenChangingToOffline() {
            // Given: 已发布公告
            when(systemNoticeMapper.selectById(2L)).thenReturn(publishedNotice);
            when(systemNoticeMapper.updateById(any(SystemNotice.class))).thenReturn(1);

            NoticeSaveRequest request = buildSaveRequest("下架公告", "下架内容", 1, 2);

            // When: 修改为下架
            adminNoticeService.update(2L, request);

            // Then: offlineTime 被设置
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).updateById(captor.capture());
            SystemNotice updated = captor.getValue();
            assertEquals(Integer.valueOf(2), updated.getNoticeStatus(), "状态应为下架(2)");
            assertNotNull(updated.getOfflineTime(), "下架时offlineTime应被设置");
        }

        /**
         * 测试修改时传非法状态值应抛出异常
         */
        @Test
        @DisplayName("修改时传非法状态应抛出异常")
        void shouldThrowWhenInvalidStatusOnUpdate() {
            when(systemNoticeMapper.selectById(2L)).thenReturn(publishedNotice);

            NoticeSaveRequest request = buildSaveRequest("测试", "内容", 1, 5);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.update(2L, request),
                    "非法状态值应抛出 BusinessException");

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("状态"));

            verify(systemNoticeMapper, never()).updateById(any(SystemNotice.class));
        }

        /**
         * 测试修改时传非法类型值应抛出异常
         */
        @Test
        @DisplayName("修改时传非法类型应抛出异常")
        void shouldThrowWhenInvalidTypeOnUpdate() {
            when(systemNoticeMapper.selectById(2L)).thenReturn(publishedNotice);

            NoticeSaveRequest request = buildSaveRequest("测试", "内容", 99, 1);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.update(2L, request),
                    "非法类型值应抛出 BusinessException");

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("类型"));

            verify(systemNoticeMapper, never()).updateById(any(SystemNotice.class));
        }
    }

    // =========================================================================
    // 状态变更测试组
    // =========================================================================

    /**
     * 状态变更测试组
     */
    @Nested
    @DisplayName("状态变更")
    class StatusTests {

        /**
         * 测试状态接口发布公告成功
         */
        @Test
        @DisplayName("状态接口发布公告应设置 publishTime")
        void shouldPublishNoticeViaStatusEndpoint() {
            // Given: 草稿公告
            when(systemNoticeMapper.selectById(1L)).thenReturn(draftNotice);
            when(systemNoticeMapper.updateById(any(SystemNotice.class))).thenReturn(1);

            NoticeStatusRequest request = new NoticeStatusRequest();
            request.setNoticeStatus(NoticeStatusEnum.PUBLISHED.getCode());

            // When: 发布公告
            adminNoticeService.updateStatus(1L, request);

            // Then: 状态变为已发布，publishTime 被设置
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).updateById(captor.capture());
            SystemNotice updated = captor.getValue();
            assertEquals(Integer.valueOf(1), updated.getNoticeStatus(), "状态应为已发布(1)");
            assertNotNull(updated.getPublishTime(), "发布时publishTime应被设置");
        }

        /**
         * 测试状态接口下架公告成功
         */
        @Test
        @DisplayName("状态接口下架公告应设置 offlineTime")
        void shouldOfflineNoticeViaStatusEndpoint() {
            // Given: 已发布公告
            when(systemNoticeMapper.selectById(2L)).thenReturn(publishedNotice);
            when(systemNoticeMapper.updateById(any(SystemNotice.class))).thenReturn(1);

            NoticeStatusRequest request = new NoticeStatusRequest();
            request.setNoticeStatus(NoticeStatusEnum.OFFLINE.getCode());

            // When: 下架公告
            adminNoticeService.updateStatus(2L, request);

            // Then: 状态变为下架，offlineTime 被设置
            ArgumentCaptor<SystemNotice> captor = ArgumentCaptor.forClass(SystemNotice.class);
            verify(systemNoticeMapper).updateById(captor.capture());
            SystemNotice updated = captor.getValue();
            assertEquals(Integer.valueOf(2), updated.getNoticeStatus(), "状态应为下架(2)");
            assertNotNull(updated.getOfflineTime(), "下架时offlineTime应被设置");
        }

        /**
         * 测试状态接口传非法状态值失败
         * <p>
         * 状态值只能是 0/1/2，传入 5 时应抛出业务异常。
         * </p>
         */
        @Test
        @DisplayName("传非法状态值应抛出异常")
        void shouldThrowWhenInvalidStatus() {
            // Given: 任意公告
            when(systemNoticeMapper.selectById(1L)).thenReturn(draftNotice);

            NoticeStatusRequest request = new NoticeStatusRequest();
            request.setNoticeStatus(5); // 非法状态

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.updateStatus(1L, request),
                    "非法状态值应抛出 BusinessException");

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode(),
                    "错误码应为 BAD_REQUEST");
            assertTrue(exception.getMessage().contains("非法"),
                    "错误信息应提示状态值非法");

            verify(systemNoticeMapper, never()).updateById(any(SystemNotice.class));
        }

        /**
         * 测试状态接口下架不存在的公告
         */
        @Test
        @DisplayName("状态变更不存在的公告应抛出 NOTICE_NOT_FOUND")
        void shouldThrowWhenStatusUpdateNotFound() {
            // Given: 公告不存在
            when(systemNoticeMapper.selectById(999L)).thenReturn(null);

            NoticeStatusRequest request = new NoticeStatusRequest();
            request.setNoticeStatus(NoticeStatusEnum.OFFLINE.getCode());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminNoticeService.updateStatus(999L, request),
                    "公告不存在时应抛出 BusinessException");

            assertEquals(ErrorCode.NOTICE_NOT_FOUND.getCode(), exception.getCode(),
                    "错误码应为 NOTICE_NOT_FOUND");
            verify(systemNoticeMapper, never()).updateById(any(SystemNotice.class));
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 构建测试用公告实体
     */
    private SystemNotice buildNotice(Long id, String title, String content,
                                      Integer status, LocalDateTime publishTime) {
        SystemNotice notice = new SystemNotice();
        notice.setId(id);
        notice.setNoticeTitle(title);
        notice.setNoticeContent(content);
        notice.setNoticeType(1);
        notice.setNoticeStatus(status);
        notice.setPublishTime(publishTime);
        notice.setPublisherId(ADMIN_USER_ID);
        notice.setCreateTime(LocalDateTime.now().minusHours(2));
        return notice;
    }

    /**
     * 构建测试用公告保存请求
     */
    private NoticeSaveRequest buildSaveRequest(String title, String content,
                                                Integer type, Integer status) {
        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setNoticeTitle(title);
        request.setNoticeContent(content);
        request.setNoticeType(type);
        request.setNoticeStatus(status);
        return request;
    }
}
