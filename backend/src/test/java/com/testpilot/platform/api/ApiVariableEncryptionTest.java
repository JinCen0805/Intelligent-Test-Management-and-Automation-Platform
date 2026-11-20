package com.testpilot.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.testpilot.platform.common.SecretCipher;
import com.testpilot.platform.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 敏感环境变量必须密文落库、接口不回明文、编辑时回传掩码不覆盖原值。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiVariableEncryptionTest {

    private static final String PLAIN_VALUE = "super-secret-token-value";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiEnvironmentVariableRepository variableRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SecretCipher secretCipher;

    @Test
    void storesSensitiveVariableEncryptedAndNeverReturnsPlaintext() throws Exception {
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();
        String auth = "Bearer " + loginToken();

        mockMvc.perform(post("/api/projects/{projectId}/api-workbench/variables", projectId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"environment":"TEST","name":"SECRET_FOR_TEST","value":"%s","sensitive":true}
                                """.formatted(PLAIN_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sensitive").value(true));

        ApiEnvironmentVariable stored = variableRepository
                .findByProjectIdAndEnvironmentAndName(projectId, "TEST", "SECRET_FOR_TEST")
                .orElseThrow();
        assertTrue(stored.getValue().startsWith("enc:v1:"), "敏感变量必须以密文前缀落库");
        assertFalse(stored.getValue().contains(PLAIN_VALUE), "数据库中不能出现明文");
        assertEquals(PLAIN_VALUE, secretCipher.decrypt(stored.getValue().substring("enc:v1:".length())));

        // 列表接口只返回掩码。
        mockMvc.perform(get("/api/projects/{projectId}/api-workbench/variables", projectId)
                        .param("environment", "TEST")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'SECRET_FOR_TEST')].value").value("••••••••"));

        // 编辑时回传掩码不能把掩码当作新值覆盖原密文。
        mockMvc.perform(post("/api/projects/{projectId}/api-workbench/variables", projectId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"environment":"TEST","name":"SECRET_FOR_TEST","value":"••••••••","sensitive":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        ApiEnvironmentVariable reloaded = variableRepository
                .findByProjectIdAndEnvironmentAndName(projectId, "TEST", "SECRET_FOR_TEST")
                .orElseThrow();
        assertEquals(PLAIN_VALUE, secretCipher.decrypt(reloaded.getValue().substring("enc:v1:".length())));
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
