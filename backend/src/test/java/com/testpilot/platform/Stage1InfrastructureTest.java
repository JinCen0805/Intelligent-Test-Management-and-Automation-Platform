package com.testpilot.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.jayway.jsonpath.JsonPath;
import com.testpilot.platform.config.RedisConfig.RedisKeyPrefix;
import com.testpilot.platform.health.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "testpilot.security.http-status-enforcement=true")
class Stage1InfrastructureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisKeyPrefix redisKeyPrefix;

    @Autowired
    private HealthService healthService;

    @Test
    void infrastructureBeansAndDependencyHealthAreAvailable() throws Exception {
        assertNotNull(mybatisPlusInterceptor);
        assertNotNull(redisTemplate);
        assertNotNull(healthService);
        assertEquals("testpilot:test:auth:session:demo", redisKeyPrefix.apply("auth:session:demo"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.checks.database.status").value("UP"))
                .andExpect(jsonPath("$.data.checks.redis.status").value("DISABLED"));
    }

    @Test
    void securityChainReturnsHttp401AndHttp403WhenEnforced() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        String developerToken = loginToken("lin.xiao");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + developerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        String adminToken = loginToken("chen.yu");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String loginToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"demo123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
