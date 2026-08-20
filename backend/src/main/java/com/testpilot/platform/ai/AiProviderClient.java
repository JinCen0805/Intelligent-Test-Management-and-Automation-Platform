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

    public String complete(AiModel model, String systemPrompt, String userPrompt) {
        String safeUserPrompt = sensitiveDataSanitizer.sanitize(userPrompt);
        String endpoint = model.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessException(40007, "AI 请求地址未配置");
        }
        String baseUrl = addressSafety.validate(endpoint).toString();
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        String apiKey = decryptApiKey(model);

        BusinessException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return callOnce(url, model.getName(), apiKey, systemPrompt, safeUserPrompt);
            } catch (BusinessException exception) {
                last = exception;
                if (exception.getCode() != 50200) {
                    throw exception;
                }
            }
        }
        throw last == null ? new BusinessException(50200, "AI 调用失败，请检查模型配置和网络") : last;
    }

    private String callOnce(String url, String modelName, String apiKey, String systemPrompt, String userPrompt) {
        try {
            JsonNode response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
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
        } catch (RestClientException exception) {
            log.warn("AI 调用失败: url={}, model={}", url, modelName, exception);
            throw new BusinessException(50200, "AI 调用失败" + detailOf(exception));
        }
    }

    private String detailOf(RestClientException exception) {
        if (exception instanceof HttpStatusCodeException httpException) {
            String body = httpException.getResponseBodyAsString();
            String snippet = body == null || body.isBlank()
                    ? ""
                    : "：" + (body.length() > 300 ? body.substring(0, 300) : body);
            return "（HTTP " + httpException.getStatusCode().value() + snippet + "）";
        }
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return "（" + cause.getMessage() + "）";
        }
        String message = exception.getMessage();
        if (message != null && !message.isBlank()) {
            return "（" + exception.getClass().getSimpleName() + "：" + message + "）";
        }
        return "（请检查模型配置和网络）";
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
