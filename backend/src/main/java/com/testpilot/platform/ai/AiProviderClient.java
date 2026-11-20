package com.testpilot.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.testpilot.platform.common.AddressSafety;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.common.SecretCipher;
import com.testpilot.platform.common.SensitiveDataSanitizer;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

/**
 * 统一的 AI Provider 客户端，兼容 OpenAI-compatible 的 {@code /chat/completions} 接口，
 * 覆盖 GPT、DeepSeek、GLM 等通过 OpenAI 协议接入的 Provider。
 *
 * <p>集中处理：Endpoint 地址安全校验、API Key 解密、连接/读取超时、失败重试。
 */
@Component
public class AiProviderClient {

    private static final String JSON_SYSTEM_PROMPT = "只输出 JSON，不要 Markdown 代码块。";
    private static final Logger log = LoggerFactory.getLogger(AiProviderClient.class);

    private final SecretCipher secretCipher;
    private final AddressSafety addressSafety;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;
    private final RestClient restClient;

    public AiProviderClient(
            SecretCipher secretCipher,
            AddressSafety addressSafety,
            SensitiveDataSanitizer sensitiveDataSanitizer,
            RestClient.Builder restClientBuilder,
            @Value("${testpilot.ai.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${testpilot.ai.read-timeout-ms:30000}") int readTimeoutMs
    ) {
        this.secretCipher = secretCipher;
        this.addressSafety = addressSafety;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    /** 以 JSON 输出约束调用模型，返回模型生成的原始文本。 */
    public String completeJson(AiModel model, String userPrompt) {
        return complete(model, JSON_SYSTEM_PROMPT, userPrompt);
    }

    public String completeJson(AiModel model, String systemPrompt, String userPrompt) {
        String prompt = systemPrompt == null || systemPrompt.isBlank()
                ? JSON_SYSTEM_PROMPT
                : systemPrompt.trim() + "\n\n" + JSON_SYSTEM_PROMPT;
        return complete(model, prompt, userPrompt);
    }

    public String complete(AiModel model, String systemPrompt, String userPrompt) {
        String safeUserPrompt = sensitiveDataSanitizer.sanitize(userPrompt);
        String endpoint = model.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessException(40007, "AI 请求地址未配置");
        }
        String baseUrl = addressSafety.validate(endpoint).toString().replaceAll("/+$", "");
        String url = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
        String apiKey = decryptApiKey(model);
        // 同一次逻辑调用在重试时复用同一 requestId，便于 Provider 侧做幂等去重。
        String requestId = java.util.UUID.randomUUID().toString();

        BusinessException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) {
                sleepBeforeRetry(attempt);
            }
            try {
                return callOnce(url, model.getName(), apiKey, systemPrompt, safeUserPrompt, requestId);
            } catch (BusinessException exception) {
                last = exception;
                if (exception.getCode() != 50200) {
                    // 4xx / 配置类错误 / 响应超时（请求已发出）重试没有意义，直接失败。
                    throw exception;
                }
            }
        }
        throw last == null ? new BusinessException(50200, "AI 调用失败，请检查模型配置和网络") : last;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(300L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String callOnce(
            String url,
            String modelName,
            String apiKey,
            String systemPrompt,
            String userPrompt,
            String requestId
    ) {
        try {
            JsonNode response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
                        headers.set("X-Request-Id", requestId);
                    })
                    .body(Map.of(
                            "model", modelName,
                            "temperature", 0.2,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content");
            if (content == null || content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(50201, "AI 返回内容为空");
            }
            return content.asText();
        } catch (HttpStatusCodeException exception) {
            int status = exception.getStatusCode().value();
            log.warn("AI 调用失败: requestId={}, url={}, model={}, httpStatus={}", requestId, url, modelName, status);
            if (status == 429 || status >= 500) {
                // 限流与服务端错误可重试。
                throw new BusinessException(50200, "AI 服务暂时不可用（HTTP " + status + "）");
            }
            // 4xx 属于配置或请求问题：不重试，且不把 Provider 响应体透传给前端/审计。
            throw new BusinessException(50203, "AI 请求被拒绝（HTTP " + status + "），请检查模型配置");
        } catch (RestClientException exception) {
            log.warn("AI 调用失败: requestId={}, url={}, model={}", requestId, url, modelName, exception);
            if (isReadTimeout(exception)) {
                // 请求已发出、读超时：自动重试会重复计费，因此不重试。
                throw new BusinessException(50204, "AI 响应超时，已停止自动重试，请稍后手动重试");
            }
            throw new BusinessException(50200, "AI 调用失败，请检查模型配置和网络");
        }
    }

    /** 区分"读超时（请求已发出）"与"连接超时（可安全重试）"。 */
    static boolean isReadTimeout(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 8) {
            if (current instanceof java.net.SocketTimeoutException) {
                String message = current.getMessage();
                String normalized = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
                return normalized.contains("read timed out") || normalized.contains("read timeout");
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }

    private String decryptApiKey(AiModel model) {
        String ciphertext = model.getApiKey();
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        try {
            return secretCipher.decrypt(ciphertext);
        } catch (BusinessException exception) {
            throw new BusinessException(50021, "AI 模型 API Key 无法解密，请重新配置");
        }
    }
}
