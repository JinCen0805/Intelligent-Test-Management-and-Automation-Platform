package com.testpilot.platform.automation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 严格模式（签名覆盖请求体）下：合法回调成功，篡改请求体、缺少 body hash、重放 nonce 都必须被拒绝。
 */
@SpringBootTest(properties = {
        "testpilot.jenkins.callback-require-body-hash=true",
        "testpilot.jenkins.callback-strict-signature=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JenkinsCallbackSignatureTest {

    private static final String TOKEN = "test-callback-token";
    private static final String JOB = "sig-job";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Test
    void verifiesBodyHashAndRejectsTamperAndReplay() throws Exception {
        TestExecution execution = testExecutionRepository.save(new TestExecution(
                987654L, 987654L, 987654L, JOB, null, "1", "QUEUED", LocalDateTime.now()
        ));
        String body = "{\"status\":\"PASSED\",\"commitSha\":\"abc\",\"totalCount\":1,"
                + "\"passedCount\":1,\"failedCount\":0,\"brokenCount\":0,\"skippedCount\":0}";
        long timestamp = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString();
        String bodyHash = sha256Hex(body);
        String signature = hmac(execution.getId() + ":" + timestamp + ":" + nonce + ":" + bodyHash + ":" + JOB);

        callback(execution.getId(), body, timestamp, nonce, bodyHash, signature)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PASSED"));

        // 同一个 nonce 重放必须被拒绝
        callback(execution.getId(), body, timestamp, nonce, bodyHash, signature)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40102));

        // 篡改请求体但沿用原签名（即使 body hash 头也改成新 body 的摘要）必须被拒绝
        String tamperedBody = body.replace("PASSED", "FAILED");
        String tamperedNonce = UUID.randomUUID().toString();
        callback(execution.getId(), tamperedBody, timestamp, tamperedNonce, sha256Hex(tamperedBody), signature)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40102));

        // 缺少 body hash 头在严格模式下必须被拒绝
        String missingHashNonce = UUID.randomUUID().toString();
        String legacySignature = hmac(execution.getId() + ":" + timestamp + ":" + missingHashNonce);
        callback(execution.getId(), body, timestamp, missingHashNonce, null, legacySignature)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40102));
    }

    private org.springframework.test.web.servlet.ResultActions callback(
            Long executionId,
            String body,
            long timestamp,
            String nonce,
            String bodyHash,
            String signature
    ) throws Exception {
        MockHttpServletRequestBuilder builder = post("/api/integrations/jenkins/callback/{executionId}", executionId)
                .header("X-Jenkins-Token", TOKEN)
                .header("X-Jenkins-Timestamp", String.valueOf(timestamp))
                .header("X-Jenkins-Nonce", nonce)
                .header("X-Jenkins-Signature", signature)
                .header("X-Jenkins-Job", JOB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (bodyHash != null) {
            builder.header("X-Jenkins-Body-SHA256", bodyHash);
        }
        return mockMvc.perform(builder);
    }

    private String sha256Hex(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String hmac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TOKEN.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
