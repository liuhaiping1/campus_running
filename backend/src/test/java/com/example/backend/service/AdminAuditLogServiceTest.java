package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.AuditLog;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.service.impl.AdminAuditLogServiceImpl;
import com.example.backend.vo.AuditLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminAuditLogService 单元测试类
 * <p>
 * Mock AuditLogMapper，不连接真实数据库。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditLogService 单元测试")
class AdminAuditLogServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AdminAuditLogServiceImpl adminAuditLogService;

    private AuditLog log1;
    private AuditLog log2;

    @BeforeEach
    void setUp() {
        log1 = buildLog(1L, "REQ-001", "ORDER", "CANCEL_ORDER",
                5L, "STUDENT", "POST", "/api/order/1/cancel",
                "192.168.1.1", "0", "取消成功", LocalDateTime.now().minusHours(2));

        log2 = buildLog(2L, "REQ-002", "RUNNER_AUTH", "REVIEW_AUTH",
                1L, "ADMIN", "POST", "/api/admin/runner-auth/1/review",
                "10.0.0.1", "0", "审核通过", LocalDateTime.now().minusHours(1));
    }

    @Nested
    @DisplayName("无筛选分页查询")
    class NoFilterTests {

        @Test
        @DisplayName("应返回全部审计日志分页")
        void shouldReturnAllAuditLogsWithoutFilter() {
            mockPage(Arrays.asList(log1, log2), 2L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, null, null, null, null, null, 1, 10);

            assertNotNull(result, "返回结果不应为null");
            assertEquals(2L, result.getTotal(), "总数应为2");
            assertEquals(2, result.getRecords().size(), "记录数应为2");
            verify(auditLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无匹配日志时应返回空页")
        void shouldReturnEmptyPageForNoMatches() {
            mockPage(new ArrayList<>(), 0L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, null, null, null, null, null, 1, 10);

            assertNotNull(result);
            assertEquals(0L, result.getTotal(), "总数应为0");
            assertTrue(result.getRecords().isEmpty(), "记录列表应为空");
        }
    }

    @Nested
    @DisplayName("精确筛选")
    class ExactFilterTests {

        @Test
        @DisplayName("应按模块筛选")
        void shouldFilterByModule() {
            mockPage(Collections.singletonList(log1), 1L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    "ORDER", null, null, null, null, null, null, null, 1, 10);

            assertEquals(1L, result.getTotal());
            assertEquals("ORDER", result.getRecords().get(0).getModule(), "模块应为ORDER");
        }

        @Test
        @DisplayName("应按动作筛选")
        void shouldFilterByAction() {
            mockPage(Collections.singletonList(log1), 1L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, "CANCEL_ORDER", null, null, null, null, null, null, 1, 10);

            assertEquals(1L, result.getTotal());
            assertEquals("CANCEL_ORDER", result.getRecords().get(0).getAction(), "动作应为CANCEL_ORDER");
        }

        @Test
        @DisplayName("应按操作人ID筛选")
        void shouldFilterByOperatorUserId() {
            mockPage(Collections.singletonList(log1), 1L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, 5L, null, null, null, null, null, 1, 10);

            assertEquals(1L, result.getTotal());
            assertEquals(Long.valueOf(5L), result.getRecords().get(0).getOperatorUserId(), "操作人ID应为5");
        }

        @Test
        @DisplayName("应按操作人角色筛选")
        void shouldFilterByOperatorRole() {
            mockPage(Collections.singletonList(log2), 1L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, "ADMIN", null, null, null, null, 1, 10);

            assertEquals(1L, result.getTotal());
            assertEquals("ADMIN", result.getRecords().get(0).getOperatorRole(), "角色应为ADMIN");
        }

        @Test
        @DisplayName("应按traceId筛选")
        void shouldFilterByTraceId() {
            mockPage(Collections.singletonList(log1), 1L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, null, "REQ-001", null, null, null, 1, 10);

            assertEquals(1L, result.getTotal());
            assertEquals("REQ-001", result.getRecords().get(0).getTraceId(), "traceId应为REQ-001");
        }
    }

    @Nested
    @DisplayName("关键词和时间筛选")
    class KeywordAndTimeTests {

        @Test
        @DisplayName("应按关键词模糊匹配")
        void shouldFilterByKeyword() {
            mockPage(Collections.singletonList(log1), 1L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, null, null, "cancel", null, null, 1, 10);

            assertEquals(1L, result.getTotal(), "关键词搜索应返回匹配结果");
        }

        @Test
        @DisplayName("空格keyword应被trim后忽略")
        void shouldIgnoreWhitespaceOnlyKeyword() {
            mockPage(Arrays.asList(log1, log2), 2L);

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, null, null, "   ", null, null, 1, 10);

            assertEquals(2L, result.getTotal(), "空格keyword应被忽略，返回全部记录");
        }

        @Test
        @DisplayName("应按时间范围筛选")
        void shouldFilterByTimeRange() {
            mockPage(Collections.singletonList(log1), 1L);

            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now();

            IPage<AuditLogVO> result = adminAuditLogService.list(
                    null, null, null, null, null, null, start, end, 1, 10);

            assertEquals(1L, result.getTotal(), "时间范围筛选应返回匹配结果");
        }
    }

    @Nested
    @DisplayName("VO字段转换")
    class VoMappingTests {

        @Test
        @DisplayName("实体字段应正确映射到VO")
        void shouldMapEntityFieldsToVo() {
            AuditLog successLog = buildLog(1L, "TRACE-OK", "ORDER", "CREATE",
                    1L, "STUDENT", "POST", "/api/order",
                    "10.0.0.1", "0", "创建成功", LocalDateTime.now());

            AuditLog failLog = buildLog(2L, "TRACE-ERR", "ORDER", "CANCEL",
                    1L, "STUDENT", "POST", "/api/order/1/cancel",
                    "10.0.0.1", "500", "订单状态不允许此操作", LocalDateTime.now());

            AuditLogVO successVO = AuditLogVO.from(successLog);
            AuditLogVO failVO = AuditLogVO.from(failLog);

            assertEquals(Long.valueOf(1L), successVO.getId(), "ID应正确映射");
            assertEquals("TRACE-OK", successVO.getTraceId(), "traceId应正确映射");
            assertEquals("ORDER", successVO.getModule(), "module应映射自moduleName");
            assertEquals("CREATE", successVO.getAction(), "action应映射自actionType");
            assertEquals("POST", successVO.getRequestMethod(), "requestMethod应正确映射");
            assertEquals("/api/order", successVO.getRequestUri(), "requestUri应映射自requestPath");
            assertEquals(Long.valueOf(1L), successVO.getOperatorUserId(), "operatorUserId应映射自operatorId");
            assertEquals("STUDENT", successVO.getOperatorRole(), "operatorRole应正确映射");
            assertEquals("10.0.0.1", successVO.getIpAddress(), "ipAddress应正确映射");

            assertTrue(successVO.getSuccess(), "resultCode=0时success应为true");
            assertNull(successVO.getErrorMessage(), "成功日志的errorMessage应为null");

            assertFalse(failVO.getSuccess(), "resultCode=500时success应为false");
            assertNotNull(failVO.getErrorMessage(), "失败日志的errorMessage不应为null");
            assertEquals("订单状态不允许此操作", failVO.getErrorMessage(), "errorMessage应为resultMsg");

            assertTrue(successVO.getOperationDesc().contains("CREATE"), "operationDesc应包含actionType");
            assertTrue(successVO.getOperationDesc().contains("创建成功"), "operationDesc应包含resultMsg");
        }
    }

    /**
     * 模拟 selectPage 返回指定数据和总数，消除重复的 thenAnswer 样板代码
     */
    @SuppressWarnings("unchecked")
    private void mockPage(List<AuditLog> records, long total) {
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    Page<AuditLog> page = inv.getArgument(0);
                    page.setRecords(records);
                    page.setTotal(total);
                    return page;
                });
    }

    private AuditLog buildLog(Long id, String traceId, String moduleName, String actionType,
                               Long operatorId, String operatorRole,
                               String requestMethod, String requestPath,
                               String ipAddress, String resultCode, String resultMsg,
                               LocalDateTime createTime) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setTraceId(traceId);
        log.setModuleName(moduleName);
        log.setActionType(actionType);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setRequestMethod(requestMethod);
        log.setRequestPath(requestPath);
        log.setIpAddress(ipAddress);
        log.setResultCode(resultCode);
        log.setResultMsg(resultMsg);
        log.setCreateTime(createTime);
        return log;
    }
}
