package com.testpilot.platform.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.rbac.RbacPermissionCache;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 项目列表必须批量取 owner 名称与成员信息，不能逐项目查询（N+1）。
 */
class ProjectListQueryTest {

    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectAccessService projectAccessService;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        userRepository = mock(UserRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        projectAccessService = mock(ProjectAccessService.class);
        projectService = new ProjectService(
                projectRepository,
                userRepository,
                projectMemberRepository,
                projectAccessService,
                mock(OperationAuditService.class),
                mock(RbacPermissionCache.class),
                mock(RbacService.class)
        );
    }

    @Test
    void listsProjectsWithBatchQueriesInsteadOfPerProjectLookups() {
        Project first = project(1L, 10L, "项目A");
        Project second = project(2L, 20L, "项目B");
        when(projectRepository.findAllByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(first, second));
        when(projectAccessService.canRead(any(Project.class), any(User.class))).thenReturn(true);
        when(userRepository.findAllById(any())).thenReturn(List.of(user(10L, "林晓"), user(20L, "陈宇")));
        when(projectMemberRepository.findAllByProjectIdIn(any()))
                .thenReturn(List.of(new ProjectMember(1L, 10L, "PROJECT_OWNER")));

        List<ProjectDtos.Response> responses = projectService.list(user(99L, "查看者"));

        assertEquals(2, responses.size());
        assertEquals("林晓", responses.get(0).ownerName());
        assertEquals(1, responses.get(0).memberCount());
        // 项目 B 没有成员记录，owner 隐式算 1 人。
        assertEquals("陈宇", responses.get(1).ownerName());
        assertEquals(1, responses.get(1).memberCount());

        verify(userRepository, times(1)).findAllById(any());
        verify(projectMemberRepository, times(1)).findAllByProjectIdIn(any());
        verify(userRepository, never()).findById(any());
        verify(projectMemberRepository, never()).countByProjectId(any());
    }

    private Project project(Long id, Long ownerId, String name) {
        Project project = new Project(name, "P" + id, "desc", ownerId, "RUNNING");
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private User user(Long id, String displayName) {
        User user = new User("u" + id, "hash", displayName, null, "TEST_DEVELOPER", "ENABLED");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
