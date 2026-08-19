package com.testpilot.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.automation.AutomationProject;
import com.testpilot.platform.automation.AutomationProjectRepository;
import com.testpilot.platform.automation.JenkinsCredentialGrant;
import com.testpilot.platform.automation.JenkinsCredentialGrantRepository;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import com.testpilot.platform.ai.AiModel;
import com.testpilot.platform.ai.AiModelRepository;
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
    private AutomationProjectRepository automationProjectRepository;

    @Autowired
    private JenkinsCredentialGrantRepository credentialGrantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiModelRepository aiModelRepository;

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
    void loginRejectsBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lin.xiao","password":"wrong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101));
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
        if (!credentialGrantRepository.existsByProjectIdAndCredentialRefAndStatus(
                projectId, "gitlab-qa-credentials", "ACTIVE")) {
            credentialGrantRepository.save(new JenkinsCredentialGrant(
                    projectId,
                    "gitlab-qa-credentials",
                    "gitlab-qa-scope",
                    userRepository.findByUsername("lin.xiao").orElseThrow().getId()
            ));
        }

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

        mockMvc.perform(put(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}",
                        projectId,
                        automationProjectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Order API Updated",
                                  "automationType":"API",
                                  "repositoryUrl":"https://example.com/order-api.git",
                                  "branch":"release/1.2",
                                  "credentialRef":"gitlab-qa-credentials",
                                  "framework":"pytest",
                                  "testPath":"tests/api",
                                  "executeCommand":"pytest tests/api --alluredir=allure-results",
                                  "environment":"STAGING",
                                  "jenkinsJob":"qa/order-api"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Order API Updated"))
                .andExpect(jsonPath("$.data.branch").value("release/1.2"))
                .andExpect(jsonPath("$.data.credentialRef").value("gitlab-qa-credentials"))
                .andExpect(jsonPath("$.data.environment").value("STAGING"))
                .andExpect(jsonPath("$.data.jenkinsJob").value("qa/order-api"));

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

        MvcResult failedExecutionResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/test-tasks/{taskId}/execute",
                        projectId,
                        taskId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        Long failedExecutionId = ((Number) JsonPath.read(
                failedExecutionResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/result",
                        projectId,
                        failedExecutionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"FAILED",
                                  "totalCount":1,
                                  "passedCount":0,
                                  "failedCount":1,
                                  "brokenCount":0,
                                  "skippedCount":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/retry",
                        projectId,
                        failedExecutionId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.id").value(Matchers.not(failedExecutionId)));
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

        mockMvc.perform(put("/api/projects/{projectId}/members/{userId}", projectId, userId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"role":"MANAGER"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MANAGER"));

        mockMvc.perform(put("/api/projects/{projectId}", projectId)
                        .header("Authorization", userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"项目管理员可编辑",
                                  "description":"manager update",
                                  "status":"RUNNING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("项目管理员可编辑"));

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

        mockMvc.perform(put("/api/projects/{projectId}/members/{userId}", projectId, ownerId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"role":"MANAGER"}
                                """.formatted(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40016));
    }

    @Test
    void apiWorkbenchPersistsCollectionsRequestsAndHistory() throws Exception {
        String auth = "Bearer " + loginToken();
        MvcResult projectsResult = mockMvc.perform(get("/api/projects")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        Long projectId = ((Number) JsonPath.read(
                projectsResult.getResponse().getContentAsString(),
                "$.data[0].id"
        )).longValue();

        MvcResult collectionResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/api-workbench/collections", projectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"订单回归集合","description":"接口回归"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("订单回归集合"))
                .andReturn();
        Long collectionId = ((Number) JsonPath.read(
                collectionResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        MvcResult requestResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/api-workbench/requests", projectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collectionId":%d,
                                  "name":"健康检查",
                                  "method":"GET",
                                  "url":"https://example.com/health",
                                  "paramsJson":"{}",
                                  "headersJson":"{}",
                                  "assertionsJson":"[]"
                                }
                """.formatted(collectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.method").value("GET"))
                .andReturn();
        Long requestId = ((Number) JsonPath.read(
                requestResult.getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(put(
                        "/api/projects/{projectId}/api-workbench/requests/{requestId}",
                        projectId,
                        requestId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collectionId":%d,
                                  "name":"健康检查",
                                  "method":"OPTIONS",
                                  "url":"https://example.com/health",
                                  "paramsJson":"{}",
                                  "headersJson":"{}",
                                  "assertionsJson":"[]"
                                }
                                """.formatted(collectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40031));

        mockMvc.perform(get(
                        "/api/projects/{projectId}/api-workbench/collections/{collectionId}/requests",
                        projectId,
                        collectionId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(1)));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/api-workbench/variables", projectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment":"TEST",
                                  "name":"base_url",
                                  "value":"https://example.com",
                                  "sensitive":false,
                                  "source":"测试"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("base_url"));
    }

    @Test
    void apiWorkbenchRejectsSsrfAndExecutesCollections() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = ((Number) JsonPath.read(
                mockMvc.perform(get("/api/projects").header("Authorization", auth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data[0].id"
        )).longValue();
        Long collectionId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/api-workbench/collections", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"SSRF 防护集合"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long requestId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/api-workbench/requests", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "collectionId":%d,
                                          "name":"内网地址",
                                          "method":"GET",
                                          "url":"http://127.0.0.1:8080/actuator",
                                          "paramsJson":"{}",
                                          "headersJson":"{}",
                                          "assertionsJson":"[]"
                                        }
                                        """.formatted(collectionId)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(post(
                        "/api/projects/{projectId}/api-workbench/requests/{requestId}/execute",
                        projectId,
                        requestId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"environment":"TEST"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.responseBody").value(org.hamcrest.Matchers.containsString("禁止访问")));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/api-workbench/collections/{collectionId}/execute",
                        projectId,
                        collectionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"environment":"TEST","stopOnFailure":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.results", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].collectionRunId").isNotEmpty());
    }

    @Test
    void reportCenterReturnsSummaryTrendAndPagination() throws Exception {
        String auth = "Bearer " + loginToken();
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", auth)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.summary.executionCount").isNumber())
                .andExpect(jsonPath("$.data.trend").isArray())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void apiHistoryReturnsStablePageShapeWhenEmpty() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        mockMvc.perform(get(
                        "/api/projects/{projectId}/api-workbench/history", projectId
                )
                        .header("Authorization", auth)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    void automationOnboardingReturnsIdAndVersionedPipelineContract() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();
        MvcResult automationProjects = mockMvc.perform(get(
                        "/api/projects/{projectId}/automation-projects", projectId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        Long automationProjectId = ((Number) JsonPath.read(
                automationProjects.getResponse().getContentAsString(), "$.data[0].id"
        )).longValue();

        mockMvc.perform(get(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/pipeline/contract",
                        projectId, automationProjectId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateVersion").value("v2"))
                .andExpect(jsonPath("$.data.parameters.TEST_ENV").value("任务环境"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/initialize",
                        projectId, automationProjectId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
    }

    @Test
    void automationOnboardingRunsThroughFailureAndRetryToReady() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        Long instanceId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/admin/jenkins-instances")
                                .header("Authorization", adminAuth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Onboarding Test Jenkins",
                                          "baseUrl":"http://jenkins-onboarding.example.com",
                                          "username":"testpilot-bot",
                                          "apiToken":"secret-token",
                                          "connectTimeoutMs":5000,
                                          "readTimeoutMs":15000,
                                          "pollIntervalMs":10000,
                                          "maxWaitMinutes":30
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long automationProjectId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/automation-projects", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Onboarding Flow Automation",
                                          "automationType":"API",
                                          "repositoryUrl":"https://example.com/onboarding.git",
                                          "branch":"main",
                                          "framework":"pytest",
                                          "testPath":"tests",
                                          "executeCommand":"pytest",
                                          "environment":"TEST",
                                          "jenkinsJob":"onboarding-legacy"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        String onboardingId = JsonPath.read(
                mockMvc.perform(post(
                                "/api/projects/{projectId}/automation-projects/{automationProjectId}/initialize",
                                projectId, automationProjectId
                        ).header("Authorization", auth))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("QUEUED"))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.onboardingId"
        );

        awaitOnboardingTerminal(projectId, automationProjectId, onboardingId, auth);

        mockMvc.perform(get(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/onboarding-status",
                        projectId, automationProjectId
                ).param("onboardingId", onboardingId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorCode").value("ONBOARDING_STEP_FAILED"))
                .andExpect(jsonPath("$.data.errorMessage").value(Matchers.containsString("Pipeline")));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/pipeline-bindings",
                        projectId, automationProjectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jenkinsInstanceId":%d,
                                  "pipelineType":"PIPELINE_JOB",
                                  "jobName":"folder/onboarding-api",
                                  "managedBy":"EXTERNAL"
                                }
                                """.formatted(instanceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobName").value("folder/onboarding-api"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/onboarding/{onboardingId}/retry",
                        projectId, automationProjectId, onboardingId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        awaitOnboardingTerminal(projectId, automationProjectId, onboardingId, auth);

        mockMvc.perform(get(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/onboarding-status",
                        projectId, automationProjectId
                ).param("onboardingId", onboardingId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.currentStep").value("DONE"))
                .andExpect(jsonPath("$.data.steps", Matchers.hasSize(7)));
    }

    private void awaitOnboardingTerminal(Long projectId, Long automationProjectId, String onboardingId, String auth) throws Exception {
        for (int i = 0; i < 50; i++) {
            String body = mockMvc.perform(get(
                            "/api/projects/{projectId}/automation-projects/{automationProjectId}/onboarding-status",
                            projectId, automationProjectId
                    ).param("onboardingId", onboardingId).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String status = JsonPath.read(body, "$.data.status");
            if ("READY".equals(status) || "FAILED".equals(status) || "CANCELED".equals(status)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("onboarding did not reach a terminal state within the timeout");
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + loginToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));
    }

    @Test
    void adminCanManageJenkinsInstanceWithoutTokenEchoAndBindPipeline() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        MvcResult instanceResult = mockMvc.perform(post("/api/admin/jenkins-instances")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Jenkins Test",
                                  "baseUrl":"http://jenkins.example.com",
                                  "username":"testpilot-bot",
                                  "apiToken":"secret-token",
                                  "connectTimeoutMs":5000,
                                  "readTimeoutMs":15000,
                                  "pollIntervalMs":10000,
                                  "maxWaitMinutes":30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenConfigured").value(true))
                .andExpect(jsonPath("$.data.apiToken").doesNotExist())
                .andReturn();
        Long instanceId = ((Number) JsonPath.read(
                instanceResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        AutomationProject automationProject = automationProjectRepository.findAll().get(0);
        mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/pipeline-bindings",
                        automationProject.getProjectId(), automationProject.getId()
                )
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jenkinsInstanceId":%d,
                                  "pipelineType":"PIPELINE_JOB",
                                  "jobName":"folder/order-api",
                                  "managedBy":"EXTERNAL"
                                }
                                """.formatted(instanceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pipelineType").value("PIPELINE_JOB"))
                .andExpect(jsonPath("$.data.jobName").value("folder/order-api"));

        mockMvc.perform(get("/api/admin/jenkins-instances")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tokenConfigured").value(true))
                .andExpect(jsonPath("$.data[0].apiToken").doesNotExist());
    }

    @Test
    void credentialGrantIsProjectScopedAndRequiredForCredentialReferences() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        String developerAuth = "Bearer " + loginToken("lin.xiao", "demo123");

        mockMvc.perform(post("/api/projects/1/jenkins-credential-grants")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credentialRef":"git-order-api",
                                  "credentialScopeRef":"git"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credentialRef").value("git-order-api"));

        mockMvc.perform(get("/api/projects/1/jenkins-credential-grants")
                        .header("Authorization", developerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].credentialRef").value("git-order-api"));

        mockMvc.perform(post("/api/projects/1/automation-projects")
                        .header("Authorization", developerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Unauthorized automation",
                                  "automationType":"API",
                                  "repositoryUrl":"https://example.com/order.git",
                                  "branch":"main",
                                  "credentialRef":"other-project-secret",
                                  "framework":"pytest",
                                  "testPath":"tests",
                                  "executeCommand":"pytest",
                                  "environment":"TEST",
                                  "jenkinsJob":"order-api"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40311));
    }

    @Test
    void aiModelApiKeyEncryptedAtRestAndNotEchoed() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        MvcResult result = mockMvc.perform(post("/api/ai/models")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"openai-gpt-4o",
                                  "provider":"OPENAI",
                                  "endpoint":"https://api.openai.com/v1",
                                  "apiKey":"sk-test-secret-123",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasApiKey").value(true))
                .andExpect(jsonPath("$.data.apiKey").doesNotExist())
                .andReturn();
        Long modelId = ((Number) JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        AiModel saved = aiModelRepository.findById(modelId).orElseThrow();
        assertNotEquals(null, saved.getApiKey());
        assertNotEquals("sk-test-secret-123", saved.getApiKey());
    }

    @Test
    void aiModelEditEnableDisableAndDelete() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        String developerAuth = "Bearer " + loginToken("lin.xiao", "demo123");

        MvcResult createResult = mockMvc.perform(post("/api/ai/models")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"gpt-4o-mini",
                                  "provider":"OPENAI",
                                  "endpoint":"https://api.openai.com/v1",
                                  "apiKey":"sk-update-secret",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasApiKey").value(true))
                .andReturn();
        Long modelId = ((Number) JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();
        String encryptedBefore = aiModelRepository.findById(modelId).orElseThrow().getApiKey();

        // 编辑：改名/Provider/Endpoint，apiKey 与 enabled 留空 → 应保留原值
        mockMvc.perform(put("/api/ai/models/{id}", modelId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"gpt-4o-mini-v2",
                                  "provider":"AZURE_OPENAI",
                                  "endpoint":"https://my.openai.azure.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("gpt-4o-mini-v2"))
                .andExpect(jsonPath("$.data.provider").value("AZURE_OPENAI"))
                .andExpect(jsonPath("$.data.endpoint").value("https://my.openai.azure.com"))
                .andExpect(jsonPath("$.data.hasApiKey").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));
        assertEquals(encryptedBefore, aiModelRepository.findById(modelId).orElseThrow().getApiKey());

        // 停用
        mockMvc.perform(put("/api/ai/models/{id}/enabled", modelId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        // 启用
        mockMvc.perform(put("/api/ai/models/{id}/enabled", modelId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        // 非管理员：新增 / 编辑 / 删除 均被拒绝
        mockMvc.perform(post("/api/ai/models")
                        .header("Authorization", developerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"forbidden","provider":"MOCK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));

        mockMvc.perform(put("/api/ai/models/{id}", modelId)
                        .header("Authorization", developerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"hijack","provider":"MOCK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));

        mockMvc.perform(delete("/api/ai/models/{id}", modelId)
                        .header("Authorization", developerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));

        // 删除不存在的模型
        mockMvc.perform(delete("/api/ai/models/{id}", 999999L)
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40420));

        // 管理员删除
        mockMvc.perform(delete("/api/ai/models/{id}", modelId)
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertTrue(aiModelRepository.findById(modelId).isEmpty());
    }

    @Test
    void aiCallAuditedAndOnlyAdminCanList() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/projects/{projectId}/automation-tasks/ai-generate", projectId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repositoryUrl":"https://example.com/audit-api.git",
                                  "branch":"main",
                                  "description":"pytest 接口自动化回归任务"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        String body = mockMvc.perform(get("/api/ai/audits")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> audits = JsonPath.read(body, "$.data");
        boolean found = audits.stream().anyMatch(audit ->
                "AUTOMATION_TASK_GENERATION".equals(audit.get("taskType"))
                        && "SUCCESS".equals(audit.get("status")));
        assertTrue(found, "应记录 AI 自动化任务生成的审计记录");

        mockMvc.perform(get("/api/ai/audits")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));
    }

    @Test
    void promptVersionManagementEditPublishAndSwitch() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");

        Long id1 = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/ai/prompts")
                                .header("Authorization", adminAuth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"版本管理测试",
                                          "taskType":"PROMPT_VERSION_TEST",
                                          "content":"版本1内容",
                                          "version":"V1.0",
                                          "enabled":true
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(0))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        // 编辑
        mockMvc.perform(put("/api/ai/prompts/{id}", id1)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"版本管理测试",
                                  "taskType":"PROMPT_VERSION_TEST",
                                  "content":"版本1内容（修订）",
                                  "version":"V1.1",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("V1.1"))
                .andExpect(jsonPath("$.data.content").value("版本1内容（修订）"));

        // 停用
        mockMvc.perform(put("/api/ai/prompts/{id}/enabled", id1)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        // 启用
        mockMvc.perform(put("/api/ai/prompts/{id}/enabled", id1)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        // 发布新版本（V2.0），旧版本自动停用
        Long id2 = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/ai/prompts/{id}/publish", id1)
                                .header("Authorization", adminAuth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"version\":\"V2.0\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.version").value("V2.0"))
                        .andExpect(jsonPath("$.data.enabled").value(true))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();
        assertNotEquals(id1, id2);

        // 版本切换：切回 V1.1，V2.0 应停用
        mockMvc.perform(post("/api/ai/prompts/{id}/activate", id1)
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id1.intValue()))
                .andExpect(jsonPath("$.data.version").value("V1.1"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        String body = mockMvc.perform(get("/api/ai/prompts")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> prompts = JsonPath.read(body, "$.data");
        for (Map<String, Object> prompt : prompts) {
            if ("版本管理测试".equals(prompt.get("name"))) {
                long pid = ((Number) prompt.get("id")).longValue();
                boolean enabled = (Boolean) prompt.get("enabled");
                if (pid == id1) {
                    assertTrue(enabled, "切回的版本应启用");
                } else if (pid == id2) {
                    assertFalse(enabled, "被替换的版本应停用");
                }
            }
        }
    }

    @Test
    void aiFailureAnalysisAnalyzesFailedExecutionWithMockModel() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        Long automationProjectId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/automation-projects", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"AI Analysis API",
                                          "automationType":"API",
                                          "repositoryUrl":"https://example.com/ai-analysis.git",
                                          "branch":"main",
                                          "framework":"pytest",
                                          "testPath":"tests",
                                          "executeCommand":"pytest",
                                          "environment":"TEST",
                                          "jenkinsJob":"ai-analysis"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long taskId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/test-tasks", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"AI Analysis regression",
                                          "taskType":"REGRESSION",
                                          "automationProjectId":%d,
                                          "environment":"TEST",
                                          "branch":"main"
                                        }
                                        """.formatted(automationProjectId)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long executionId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/execute", projectId, taskId)
                                .header("Authorization", auth))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("QUEUED"))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/analyze",
                        projectId,
                        executionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40018));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/result",
                        projectId,
                        executionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"FAILED",
                                  "totalCount":2,
                                  "passedCount":0,
                                  "failedCount":1,
                                  "brokenCount":1,
                                  "skippedCount":0,
                                  "failureDetail":"selenium.common.exceptions.WebDriverException: 'chromedriver' executable needs to be in PATH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/analyze",
                        projectId,
                        executionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.provider").value("MOCK"))
                .andExpect(jsonPath("$.data.category").value("ENVIRONMENT"))
                .andExpect(jsonPath("$.data.categoryLabel").value("环境问题"))
                .andExpect(jsonPath("$.data.possibleCauses", Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.data.relatedErrors", Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());

        mockMvc.perform(post(
                        "/api/projects/{projectId}/test-executions/{executionId}/analyze",
                        projectId,
                        executionId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void pipelineBindingListUpdateAndTaskSelection() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        Long instanceId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/admin/jenkins-instances")
                                .header("Authorization", adminAuth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Binding Test Jenkins",
                                          "baseUrl":"http://jenkins-binding.example.com",
                                          "username":"testpilot-bot",
                                          "apiToken":"secret-token",
                                          "connectTimeoutMs":5000,
                                          "readTimeoutMs":15000,
                                          "pollIntervalMs":10000,
                                          "maxWaitMinutes":30
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long automationProjectId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/automation-projects", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Binding Test Automation",
                                          "automationType":"API",
                                          "repositoryUrl":"https://example.com/binding.git",
                                          "branch":"main",
                                          "framework":"pytest",
                                          "testPath":"tests",
                                          "executeCommand":"pytest",
                                          "environment":"TEST",
                                          "jenkinsJob":"binding-legacy"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long bindingId = ((Number) JsonPath.read(
                mockMvc.perform(post(
                                "/api/projects/{projectId}/automation-projects/{automationProjectId}/pipeline-bindings",
                                projectId, automationProjectId
                        )
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "jenkinsInstanceId":%d,
                                          "pipelineType":"PIPELINE_JOB",
                                          "jobName":"folder/order-api",
                                          "managedBy":"EXTERNAL"
                                        }
                                        """.formatted(instanceId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.jobName").value("folder/order-api"))
                        .andExpect(jsonPath("$.data.managedBy").value("EXTERNAL"))
                        .andExpect(jsonPath("$.data.pipelineType").value("PIPELINE_JOB"))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(get(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/pipeline-bindings",
                        projectId, automationProjectId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[0].jobName").value("folder/order-api"));

        mockMvc.perform(get(
                        "/api/projects/{projectId}/automation-projects/{automationProjectId}/jenkins-jobs",
                        projectId, automationProjectId
                )
                        .param("jenkinsInstanceId", String.valueOf(instanceId))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(0)));

        mockMvc.perform(put("/api/projects/{projectId}/pipeline-bindings/{bindingId}", projectId, bindingId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jenkinsInstanceId":%d,
                                  "pipelineType":"PIPELINE_JOB",
                                  "jobName":"folder/order-api-v2",
                                  "managedBy":"PLATFORM"
                                }
                                """.formatted(instanceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobName").value("folder/order-api-v2"));

        mockMvc.perform(post("/api/projects/{projectId}/test-tasks", projectId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Binding Task",
                                  "taskType":"REGRESSION",
                                  "automationProjectId":%d,
                                  "pipelineBindingId":%d,
                                  "environment":"TEST",
                                  "branch":"main"
                                }
                                """.formatted(automationProjectId, bindingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pipelineBindingId").value(bindingId))
                .andExpect(jsonPath("$.data.jenkinsJob").value("folder/order-api-v2"));
    }

    @Test
    void testTaskLifecycleEditDisableStopAndDelete() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        Long automationProjectId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/automation-projects", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Lifecycle Automation",
                                          "automationType":"API",
                                          "repositoryUrl":"https://example.com/lifecycle.git",
                                          "branch":"main",
                                          "framework":"pytest",
                                          "testPath":"tests",
                                          "executeCommand":"pytest",
                                          "environment":"TEST",
                                          "jenkinsJob":"qa/lifecycle"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long taskId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/test-tasks", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Lifecycle regression",
                                          "taskType":"REGRESSION",
                                          "automationProjectId":%d,
                                          "environment":"TEST",
                                          "branch":"main",
                                          "parametersJson":"{\\"suite\\":\\"smoke\\"}"
                                        }
                                        """.formatted(automationProjectId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("ENABLED"))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(put("/api/projects/{projectId}/test-tasks/{taskId}", projectId, taskId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Lifecycle regression updated",
                                  "taskType":"SMOKE",
                                  "automationProjectId":%d,
                                  "environment":"STAGING",
                                  "branch":"release/1.0",
                                  "parametersJson":"{\\"suite\\":\\"regression\\"}"
                                }
                                """.formatted(automationProjectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lifecycle regression updated"))
                .andExpect(jsonPath("$.data.taskType").value("SMOKE"))
                .andExpect(jsonPath("$.data.environment").value("STAGING"))
                .andExpect(jsonPath("$.data.branch").value("release/1.0"));

        mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/status", projectId, taskId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/status", projectId, taskId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/status", projectId, taskId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40022));

        Long executionId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/execute", projectId, taskId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"parametersJson\":\"{\\\"suite\\\":\\\"override\\\"}\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("QUEUED"))
                        .andExpect(jsonPath("$.data.triggerMethod").value("MANUAL"))
                        .andExpect(jsonPath("$.data.triggeredBy").value("lin.xiao"))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/stop", projectId, taskId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABORTED"))
                .andExpect(jsonPath("$.data.id").value(executionId.intValue()));

        mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/stop", projectId, taskId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40020));

        mockMvc.perform(delete("/api/projects/{projectId}/test-tasks/{taskId}", projectId, taskId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void reportExecutionDetailAndConsoleLog() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        Long automationProjectId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/automation-projects", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Detail Automation",
                                          "automationType":"API",
                                          "repositoryUrl":"https://example.com/detail.git",
                                          "branch":"main",
                                          "framework":"pytest",
                                          "testPath":"tests",
                                          "executeCommand":"pytest",
                                          "environment":"TEST",
                                          "jenkinsJob":"qa/detail"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long taskId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/test-tasks", projectId)
                                .header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Detail regression",
                                          "taskType":"REGRESSION",
                                          "automationProjectId":%d,
                                          "environment":"STAGING",
                                          "branch":"release/1.0"
                                        }
                                        """.formatted(automationProjectId)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        Long executionId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects/{projectId}/test-tasks/{taskId}/execute", projectId, taskId)
                                .header("Authorization", auth))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("QUEUED"))
                        .andReturn().getResponse().getContentAsString(),
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
                                  "status":"FAILED",
                                  "commitSha":"def456",
                                  "consoleLogRef":"mock://jenkins/log/%d",
                                  "totalCount":2,
                                  "passedCount":0,
                                  "failedCount":1,
                                  "brokenCount":1,
                                  "skippedCount":0,
                                  "failureDetail":"WebDriverException: chromedriver not in PATH"
                                }
                                """.formatted(executionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(get("/api/reports/executions/{executionId}", executionId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(executionId.intValue()))
                .andExpect(jsonPath("$.data.projectName").isNotEmpty())
                .andExpect(jsonPath("$.data.taskName").value("Detail regression"))
                .andExpect(jsonPath("$.data.taskType").value("REGRESSION"))
                .andExpect(jsonPath("$.data.automationProjectName").value("Detail Automation"))
                .andExpect(jsonPath("$.data.branch").value("release/1.0"))
                .andExpect(jsonPath("$.data.environment").value("STAGING"))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.triggeredBy").value("lin.xiao"))
                .andExpect(jsonPath("$.data.triggerMethod").value("MANUAL"))
                .andExpect(jsonPath("$.data.report.failedCount").value(1))
                .andExpect(jsonPath("$.data.report.brokenCount").value(1));

        mockMvc.perform(get(
                        "/api/projects/{projectId}/test-executions/{executionId}/console-log",
                        projectId,
                        executionId
                ).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(Matchers.containsString("[ERROR]")));
    }

    @Test
    void aiCreatesAutomationTaskDraftAndConfirms() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        MvcResult draftResult = mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-tasks/ai-generate", projectId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repositoryUrl":"https://example.com/order-api.git",
                                  "branch":"main",
                                  "description":"这是我的 pytest 接口自动化仓库，请创建 TEST 环境回归任务，执行 tests/regression，并生成 Allure 报告。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("MOCK"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.automationType").value("API"))
                .andExpect(jsonPath("$.data.framework").value("PYTEST"))
                .andExpect(jsonPath("$.data.taskType").value("REGRESSION"))
                .andExpect(jsonPath("$.data.environment").value("TEST"))
                .andExpect(jsonPath("$.data.testPath").value("tests/regression"))
                .andExpect(jsonPath("$.data.allureEnabled").value(true))
                .andExpect(jsonPath("$.data.missingItems", Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.data.riskHints", Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty())
                .andReturn();
        Long draftId = ((Number) JsonPath.read(
                draftResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        mockMvc.perform(put(
                        "/api/projects/{projectId}/automation-tasks/ai-drafts/{draftId}",
                        projectId,
                        draftId
                )
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"订单接口回归（已确认）",
                                  "taskType":"REGRESSION",
                                  "automationType":"API",
                                  "repositoryUrl":"https://example.com/order-api.git",
                                  "branch":"main",
                                  "framework":"PYTEST",
                                  "testPath":"tests/regression",
                                  "executeCommand":"pytest tests/regression --alluredir=allure-results",
                                  "environment":"STAGING",
                                  "parametersJson":"{\\"testPath\\":\\"tests/regression\\"}",
                                  "pipelineType":"PIPELINE",
                                  "jenkinsJob":"order-api-regression",
                                  "allureEnabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("订单接口回归（已确认）"))
                .andExpect(jsonPath("$.data.environment").value("STAGING"))
                .andExpect(jsonPath("$.data.pipelineType").value("PIPELINE"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-tasks/ai-drafts/{draftId}/confirm",
                        projectId,
                        draftId
                )
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.draftId").value(draftId.intValue()))
                .andExpect(jsonPath("$.data.automationProjectId").isNotEmpty())
                .andExpect(jsonPath("$.data.testTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.task.name").value("订单接口回归（已确认）"))
                .andExpect(jsonPath("$.data.task.environment").value("STAGING"))
                .andExpect(jsonPath("$.data.task.jenkinsJob").value("order-api-regression"))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());

        mockMvc.perform(post(
                        "/api/projects/{projectId}/automation-tasks/ai-drafts/{draftId}/confirm",
                        projectId,
                        draftId
                )
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40023));
    }

    @Test
    void apiWorkbenchExecutesRealRequestEndToEnd() throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("localhost"), 0), 0);
        server.createContext("/health", exchange -> {
            byte[] body = "{\"status\":\"ok\",\"data\":{\"id\":123,\"name\":\"order\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        Long projectId = null;
        try {
            String auth = "Bearer " + loginToken();
            String code = "API-E2E-" + System.nanoTime();
            projectId = ((Number) JsonPath.read(
                    mockMvc.perform(post("/api/projects")
                                    .header("Authorization", auth)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"name":"接口工作台 E2E","code":"%s","description":"真实执行验收"}
                                            """.formatted(code)))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    "$.data.id"
            )).longValue();

            // 环境变量：base_url 真实回显，token 敏感脱敏。
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            mockMvc.perform(post("/api/projects/{projectId}/api-workbench/variables", projectId)
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"environment":"TEST","name":"base_url","value":"%s","sensitive":false}
                                    """.formatted(baseUrl)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.value").value(baseUrl));

            mockMvc.perform(post("/api/projects/{projectId}/api-workbench/variables", projectId)
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"environment":"TEST","name":"token","value":"secret-token-value","sensitive":true}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.value").value("••••••••"))
                    .andExpect(jsonPath("$.data.sensitive").value(true));

            Long collectionId = ((Number) JsonPath.read(
                    mockMvc.perform(post("/api/projects/{projectId}/api-workbench/collections", projectId)
                                    .header("Authorization", auth)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"name\":\"真实执行集合\"}"))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    "$.data.id"
            )).longValue();

            Map<String, Object> requestPayload = Map.<String, Object>of(
                    "collectionId", collectionId,
                    "name", "健康检查",
                    "method", "GET",
                    "url", "{{base_url}}/health",
                    "paramsJson", "{\"trace\":\"{{trace_id}}\"}",
                    "headersJson", "{\"Authorization\":\"Bearer {{token}}\"}",
                    "assertionsJson", objectMapper.writeValueAsString(List.of(
                            Map.of("type", "status", "expected", "200", "enabled", true),
                            Map.of("type", "json", "path", "$.status", "expected", "ok", "enabled", true),
                            Map.of("type", "field", "path", "$.data.id", "expected", "123", "enabled", true),
                            Map.of("type", "field", "path", "$.data", "valueType", "OBJECT", "exists", true, "enabled", true),
                            Map.of("type", "contains", "expected", "order", "enabled", true)
                    )),
                    "preRequestScript", "set trace_id=abc123",
                    "postRequestScript", "extract order_id=$.data.id"
            );

            Long requestId = ((Number) JsonPath.read(
                    mockMvc.perform(post("/api/projects/{projectId}/api-workbench/requests", projectId)
                                    .header("Authorization", auth)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requestPayload)))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    "$.data.id"
            )).longValue();

            mockMvc.perform(post(
                            "/api/projects/{projectId}/api-workbench/requests/{requestId}/execute",
                            projectId,
                            requestId
                    )
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"environment\":\"TEST\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PASSED"))
                    .andExpect(jsonPath("$.data.statusCode").value(200))
                    .andExpect(jsonPath("$.data.responseBody").value(Matchers.containsString("order")))
                    .andExpect(jsonPath("$.data.requestSnapshotJson").isNotEmpty())
                    .andExpect(jsonPath("$.data.assertionResultsJson").value(Matchers.containsString("\"passed\":true")))
                    .andExpect(jsonPath("$.data.assertionResultsJson").value(Matchers.not(Matchers.containsString("\"passed\":false"))));

            mockMvc.perform(post(
                            "/api/projects/{projectId}/api-workbench/collections/{collectionId}/execute",
                            projectId,
                            collectionId
                    )
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"environment\":\"TEST\",\"stopOnFailure\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PASSED"))
                    .andExpect(jsonPath("$.data.results", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.data.results[0].collectionRunId").isNotEmpty());

            mockMvc.perform(get(
                            "/api/projects/{projectId}/api-workbench/history", projectId
                    )
                            .header("Authorization", auth)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(Matchers.greaterThanOrEqualTo(2)));
        } finally {
            server.stop(0);
            if (projectId != null) {
                projectRepository.findById(projectId).ifPresent(project -> {
                    project.setDeleted(true);
                    projectRepository.save(project);
                });
            }
        }
    }

    @Test
    void operationAuditedAndOnlyAdminCanList() throws Exception {
        String adminAuth = "Bearer " + loginToken("chen.yu", "demo123");
        String username = "op.audit." + System.nanoTime();
        mockMvc.perform(post("/api/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"demo123",
                                  "displayName":"操作审计用户",
                                  "email":"op.audit@example.com",
                                  "role":"TEST_DEVELOPER"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String body = mockMvc.perform(get("/api/operation-audits")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> audits = JsonPath.read(body, "$.data");
        boolean loginFound = audits.stream().anyMatch(audit ->
                "LOGIN".equals(audit.get("action")) && "SUCCESS".equals(audit.get("result")));
        boolean userCreateFound = audits.stream().anyMatch(audit ->
                "USER_CREATE".equals(audit.get("action")));
        assertTrue(loginFound, "应记录登录操作审计");
        assertTrue(userCreateFound, "应记录用户创建操作审计");

        mockMvc.perform(get("/api/operation-audits")
                        .header("Authorization", "Bearer " + loginToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40303));
    }

    @Test
    void fileUploadValidatesEmptyAndUnsupportedType() throws Exception {
        String auth = "Bearer " + loginToken();
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();

        mockMvc.perform(multipart("/api/projects/{projectId}/requirements/upload", projectId)
                        .file(new MockMultipartFile("file", "empty.md", "text/markdown", new byte[0]))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003));

        mockMvc.perform(multipart("/api/projects/{projectId}/requirements/upload", projectId)
                        .file(new MockMultipartFile(
                                "file",
                                "malware.exe",
                                "application/octet-stream",
                                "not-a-requirement".getBytes(StandardCharsets.UTF_8)
                        ))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void projectCodeValidatesFormatAndDuplicate() throws Exception {
        String auth = "Bearer " + loginToken();

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"非法编码","code":"BAD CODE!","description":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40026));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"重复编码","code":"XH-MALL","description":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901));
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
