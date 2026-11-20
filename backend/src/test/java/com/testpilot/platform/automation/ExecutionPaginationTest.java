package com.testpilot.platform.automation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.testpilot.platform.project.ProjectRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 执行记录列表必须分页返回，避免一次性返回全部历史。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExecutionPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void paginatesExecutionsByProject() throws Exception {
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();
        for (int index = 0; index < 3; index++) {
            testExecutionRepository.save(new TestExecution(
                    projectId,
                    666L + index,
                    666L,
                    "page-job-" + index,
                    null,
                    String.valueOf(index),
                    "PASSED",
                    LocalDateTime.now()
            ));
        }
        String auth = "Bearer " + loginToken();

        mockMvc.perform(get("/api/projects/{projectId}/test-executions", projectId)
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalPages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/projects/{projectId}/test-executions", projectId)
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    private String loginToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lin.xiao","password":"demo123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
