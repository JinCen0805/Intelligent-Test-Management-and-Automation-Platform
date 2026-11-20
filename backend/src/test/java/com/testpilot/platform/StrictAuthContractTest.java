package com.testpilot.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 严格鉴权契约：保留真实 HTTP 401/403，不依赖测试 profile 的 permitAll 兼容模式。
 * 用于防止"某个接口忘记鉴权/权限判断被绕过"的回归。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "test-strict"})
class StrictAuthContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestGetsHttp401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void developerCannotAccessSystemUserManagement() throws Exception {
        String developer = "Bearer " + loginToken("lin.xiao", "demo123");

        mockMvc.perform(get("/api/users").header("Authorization", developer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void developerCannotReadProjectOutsideMembership() throws Exception {
        String admin = "Bearer " + loginToken("chen.yu", "demo123");
        String code = "STRICT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Long projectId = ((Number) JsonPath.read(
                mockMvc.perform(post("/api/projects")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"严格鉴权测试项目","code":"%s","description":""}
                                        """.formatted(code)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(0))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.id"
        )).longValue();

        String developer = "Bearer " + loginToken("lin.xiao", "demo123");
        mockMvc.perform(get("/api/projects/{id}", projectId).header("Authorization", developer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void adminCanAccessUserManagement() throws Exception {
        String admin = "Bearer " + loginToken("chen.yu", "demo123");

        mockMvc.perform(get("/api/users").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
