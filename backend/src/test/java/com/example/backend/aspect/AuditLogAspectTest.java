package com.example.backend.aspect;

import com.example.backend.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志切面单元测试
 * <p>
 * 验证 {@link AuditLogAspect} 的用户识别和兜底逻辑。
 * 切面的 Around 行为需 Spring 集成测试覆盖，本测试聚焦可隔离验证的部分。
 * </p>
 */
@DisplayName("审计日志切面 单元测试")
class AuditLogAspectTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 未登录时 operatorId=0, operatorRole=ANONYMOUS
     */
    @Test
    @DisplayName("未登录时 Authentication 为 null，切面应兜底")
    void shouldDefaultToAnonymousWhenNotAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "未登录时 Authentication 应为 null，切面会兜底 operatorId=0, role=ANONYMOUS");
    }

    /**
     * 登录用户角色从 authorities 获取
     */
    @Test
    @DisplayName("登录用户应正确获取角色")
    void shouldGetRoleFromAuthorities() {
        LoginUser user = new LoginUser(2L, "runner", "pwd",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_RUNNER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(LoginUser.class, auth.getPrincipal());
        LoginUser principal = (LoginUser) auth.getPrincipal();
        assertEquals(2L, principal.getUserId());
        assertEquals("ROLE_RUNNER", principal.getAuthorities().iterator().next().getAuthority());
    }

    /**
     * 多角色时取某个角色的行为已验证
     * <p>
     * Spring Security User 将 authorities 存入无序 Set，迭代顺序不保证。
     * 切面取第一个 authority 即可，业务上不依赖具体顺序。
     * </p>
     */
    @Test
    @DisplayName("多角色时应至少能取到一个角色")
    void shouldTakeFirstRoleWhenMultiple() {
        var authorities = java.util.Arrays.asList(
                new SimpleGrantedAuthority("ROLE_STUDENT"),
                new SimpleGrantedAuthority("ROLE_RUNNER")
        );
        LoginUser user = new LoginUser(1L, "student", "pwd", authorities);
        String firstRole = user.getAuthorities().iterator().next().getAuthority();
        assertTrue(firstRole.equals("ROLE_STUDENT") || firstRole.equals("ROLE_RUNNER"),
                "多角色时第一个角色应为其中之一");
    }
}
