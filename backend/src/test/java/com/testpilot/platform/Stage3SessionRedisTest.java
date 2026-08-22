package com.testpilot.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.testpilot.platform.auth.AuthDtos;
import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.auth.AuthSessionStore;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.rbac.RbacPermissionCache;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import com.testpilot.platform.user.UserService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Stage3SessionRedisTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthSessionStore authSessionStore;

    @Autowired
    private RbacPermissionCache permissionCache;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginStoresSessionWithExpectedKeyAndLogoutRemovesIt() {
        User user = createEnabledUser("session." + UUID.randomUUID());

        AuthDtos.LoginResponse response = authService.login(new AuthDtos.LoginRequest(user.getUsername(), "demo123"));
        String token = response.accessToken();

        assertEquals("testpilot:test:auth:session:" + token, authSessionStore.sessionKey(token));
        assertNotNull(authSessionStore.get(token));
        assertEquals(1L, authSessionStore.get(token).authVersion());
        assertEquals(user.getId(), authService.currentUser("Bearer " + token).getId());

        authService.logout("Bearer " + token);
        assertThrows(BusinessException.class, () -> authService.currentUser("Bearer " + token));
        assertFalse(authSessionStore.get(token) != null);
    }

    @Test
    void passwordResetBumpsAuthVersionAndInvalidatesOldToken() {
        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        User user = createEnabledUser("password." + UUID.randomUUID());
        String token = authService.login(new AuthDtos.LoginRequest(user.getUsername(), "demo123")).accessToken();

        userService.resetPassword(user.getId(), new com.testpilot.platform.user.UserDtos.PasswordRequest("demo456"), admin);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.currentUser("Bearer " + token));
        assertEquals(40100, exception.getCode());

        assertTrue(authService.login(new AuthDtos.LoginRequest(user.getUsername(), "demo456")).accessToken().length() > 10);
        assertTrue(userRepository.findById(user.getId()).orElseThrow().getAuthVersion() > 1L);
    }

    @Test
    void projectPermissionCacheIsVersionedAndInvalidatedOnRoleChange() {
        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        User user = createEnabledUser("rbac." + UUID.randomUUID());
        Project project = projectRepository.save(new Project(
                "阶段3项目",
                "S3-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                "阶段3权限缓存校验",
                admin.getId(),
                "RUNNING"
        ));

        rbacService.assignProjectRole(admin, user.getId(), project.getId(), "PROJECT_VIEWER");
        Long firstVersion = userRepository.findById(user.getId()).orElseThrow().getAuthVersion();
        Set<String> firstPermissions = rbacService.projectPermissions(user.getId(), project.getId());
        assertTrue(firstPermissions.contains("project:read"));
        assertEquals("testpilot:test:rbac:project:" + project.getId() + ":user:" + user.getId(),
                permissionCache.projectKey(user.getId(), project.getId()));
        assertTrue(permissionCache.getProject(user.getId(), project.getId(), firstVersion).isPresent());

        rbacService.assignProjectRole(admin, user.getId(), project.getId(), "PROJECT_MANAGER");
        Long secondVersion = userRepository.findById(user.getId()).orElseThrow().getAuthVersion();
        Set<String> secondPermissions = rbacService.projectPermissions(user.getId(), project.getId());

        assertTrue(secondVersion > firstVersion);
        assertTrue(secondPermissions.contains("project:update"));
        assertTrue(permissionCache.getProject(user.getId(), project.getId(), secondVersion).isPresent());
    }

    @Test
    void permissionCacheKeyFormatsAreStable() {
        User user = userRepository.findByUsername("lin.xiao").orElseThrow();
        Project project = projectRepository.findByCodeAndDeletedFalse("XH-MALL").orElseThrow();

        assertEquals("testpilot:test:rbac:user:" + user.getId(), permissionCache.systemKey(user.getId()));
        assertEquals("testpilot:test:rbac:project:" + project.getId() + ":user:" + user.getId(),
                permissionCache.projectKey(user.getId(), project.getId()));
        assertEquals("testpilot:test:auth:session:abc", authSessionStore.sessionKey("abc"));
    }

    private User createEnabledUser(String username) {
        return userRepository.save(new User(
                username,
                passwordEncoder.encode("demo123"),
                username,
                username + "@example.com",
                "TEST_DEVELOPER",
                "ENABLED"
        ));
    }
}
