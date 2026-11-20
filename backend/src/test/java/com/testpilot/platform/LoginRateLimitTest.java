package com.testpilot.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginRateLimitTest {

    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void locksAccountAfterRepeatedFailures() throws Exception {
        String ip = "198.51.100.10";
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            mockMvc.perform(login("lin.xiao", "wrong-password", ip))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40101));
        }

        // 超过阈值后，即使密码正确也必须被限流拦截。
        mockMvc.perform(login("lin.xiao", "demo123", ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(42901));

        // 其他 IP 不受影响。
        mockMvc.perform(login("lin.xiao", "demo123", "198.51.100.11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        String ip = "198.51.100.12";
        for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
            mockMvc.perform(login("lin.xiao", "wrong-password", ip))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40101));
        }

        mockMvc.perform(login("lin.xiao", "demo123", ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
            mockMvc.perform(login("lin.xiao", "wrong-password", ip))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40101));
        }
    }

    private MockHttpServletRequestBuilder login(String username, String password, String ip) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password))
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }
}
