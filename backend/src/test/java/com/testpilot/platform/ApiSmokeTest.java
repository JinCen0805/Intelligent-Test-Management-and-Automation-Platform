package com.testpilot.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void loginReturnsAccessTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lin.xiao","password":"demo123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("lin.xiao"));
    }

    @Test
    void p0BusinessFlowCreatesDraftReviewsAndExecutes() throws Exception {
        String token = loginToken();
        String auth = "Bearer " + token;

        MvcResult projectResult = mockMvc.perform(get("/api/projects")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)))
                .andReturn();
        Long projectId = ((Number) JsonPath.read(
                projectResult.getResponse().getContentAsString(),
                "$.data[0].id"
        )).longValue();

        MvcResult uploadResult = mockMvc.perform(multipart("/api/projects/{projectId}/requirements/upload", projectId)
                        .file(new MockMultipartFile(
                                "file",
                                "order-requirement.md",
                                "text/markdown",
                                "# Order\n\nUsers can create an order with valid stock."
                                        .getBytes()
                        ))
                        .param("version", "V1.0")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parseStatus").value("PARSED"))
                .andReturn();
        Long requirementId = ((Number) JsonPath.read(
                uploadResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(post(
                        "/api/projects/{projectId}/requirements/{requirementId}/test-cases/generate",
                        projectId,
                        requirementId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("MOCK"))
                .andExpect(jsonPath("$.data.testCases", Matchers.hasSize(3)));

        MvcResult casesResult = mockMvc.perform(get(
                        "/api/projects/{projectId}/test-cases",
                        projectId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andReturn();
        Long testCaseId = ((Number) JsonPath.read(
                casesResult.getResponse().getContentAsString(),
                "$.data.content[0].id"
        )).longValue();

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-cases/{testCaseId}/status",
                        projectId,
                        testCaseId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40005));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-cases/{testCaseId}/status",
                        projectId,
                        testCaseId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEW"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-cases/{testCaseId}/status",
                        projectId,
                        testCaseId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/projects/{projectId}/test-cases", projectId)
                        .param("priority", "P9")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40011));

        MvcResult automationResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-projects",
                        projectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Order API",
                                  "automationType":"API",
                                  "repositoryUrl":"https://example.com/order-api.git",
                                  "branch":"main",
                                  "framework":"pytest",
                                  "testPath":"tests/api",
                                  "executeCommand":"pytest tests/api",
                                  "environment":"TEST",
                                  "jenkinsJob":"order-api"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Long automationProjectId = ((Number) JsonPath.read(
                automationResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        MvcResult taskResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/test-tasks",
                        projectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Order regression",
                                  "taskType":"REGRESSION",
                                  "automationProjectId":%d,
                                  "environment":"TEST",
                                  "branch":"main"
                                }
                                """.formatted(automationProjectId)))
                .andExpect(status().isOk())
                .andReturn();
        Long taskId = ((Number) JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        MvcResult executionResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/test-tasks/{taskId}/execute",
                        projectId,
                        taskId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.buildNumber").isNotEmpty())
                .andReturn();
        Long executionId = ((Number) JsonPath.read(
                executionResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/result",
                        projectId,
                        executionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"PASSED",
                                  "totalCount":2,
                                  "passedCount":1,
                                  "failedCount":0,
                                  "brokenCount":0,
                                  "skippedCount":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40010));

        mockMvc.perform(post(
                        "/api/integrations/jenkins/callback/{executionId}",
                        executionId
                )
                        .header("X-Jenkins-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"PASSED",
                                  "commitSha":"abc123",
                                  "consoleLogRef":"mock://jenkins/log",
                                  "allureUrl":"http://allure.local/order",
                                  "totalCount":3,
                                  "passedCount":3,
                                  "failedCount":0,
                                  "brokenCount":0,
                                  "skippedCount":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40102));

        mockMvc.perform(post(
                        "/api/integrations/jenkins/callback/{executionId}",
                        executionId
                )
                        .header("X-Jenkins-Token", "test-callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"PASSED",
                                  "commitSha":"abc123",
                                  "consoleLogRef":"mock://jenkins/log",
                                  "allureUrl":"http://allure.local/order",
                                  "totalCount":3,
                                  "passedCount":3,
                                  "failedCount":0,
                                  "brokenCount":0,
                                  "skippedCount":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PASSED"))
                .andExpect(jsonPath("$.data.report.totalCount").value(3));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/sync",
                        projectId,
                        executionId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PASSED"));
    }

    @Test
    void projectAccessDeniesUserOutsideProject() throws Exception {
        User outsider = userRepository.findByUsername("qa.outsider")
                .orElseGet(() -> userRepository.save(new User(
                        "qa.outsider",
                        passwordEncoder.encode("demo123"),
                        "越权用户",
                        "qa.outsider@example.com",
                        "TEST_DEVELOPER",
                        "ENABLED"
                )));
        Project project = projectRepository.save(new Project(
                "隔离项目",
                "ISOLATED-" + System.nanoTime(),
                "权限测试项目",
                outsider.getId(),
                "TEST",
                "RUNNING"
        ));

        mockMvc.perform(get("/api/projects/{projectId}", project.getId())
                        .header("Authorization", "Bearer " + loginToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void p1AdminUserAndProjectMemberManagement() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        MvcResult projectsResult = mockMvc.perform(get("/api/projects")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andReturn();
        Long projectId = ((Number) JsonPath.read(
                projectsResult.getResponse().getContentAsString(),
                "$.data[0].id"
        )).longValue();

        String username = "p1.user." + System.nanoTime();
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"demo123",
                                  "displayName":"P1 测试用户",
                                  "email":"p1@example.com",
                                  "role":"TEST_DEVELOPER"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andReturn();
        Long userId = ((Number) JsonPath.read(
                userResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();
        String userAuth = "Bearer " + loginToken(username, "demo123");

        mockMvc.perform(post("/api/users/{userId}/status", userId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(post("/api/users/{userId}/status", userId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"role":"VIEWER"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("VIEWER"));

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"role":"VIEWER"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40904));

        mockMvc.perform(delete("/api/projects/{projectId}/members/{userId}", projectId, userId)
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long ownerId = ((Number) JsonPath.read(
                projectsResult.getResponse().getContentAsString(),
                "$.data[0].ownerId"
        )).longValue();
        mockMvc.perform(delete("/api/projects/{projectId}/members/{userId}", projectId, ownerId)
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40016));
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + loginToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));
    }

    private String loginToken() throws Exception {
        return loginToken("lin.xiao", "demo123");
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.accessToken"
        );
    }
}
