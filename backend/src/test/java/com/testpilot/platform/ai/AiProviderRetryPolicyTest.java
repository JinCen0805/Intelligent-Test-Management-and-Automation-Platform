package com.testpilot.platform.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

/**
 * AI 重试语义：只有"连接超时/网络异常"可安全重试；读超时说明请求已发出，重试可能重复计费。
 */
class AiProviderRetryPolicyTest {

    @Test
    void readTimeoutIsNotRetried() {
        SocketTimeoutException exception = new SocketTimeoutException("Read timed out");
        assertTrue(AiProviderClient.isReadTimeout(exception));
    }

    @Test
    void readTimeoutDetectedThroughNestedCause() {
        RuntimeException wrapper = new RuntimeException("request failed",
                new SocketTimeoutException("read timeout while reading response"));
        assertTrue(AiProviderClient.isReadTimeout(wrapper));
    }

    @Test
    void connectTimeoutIsRetryable() {
        SocketTimeoutException exception = new SocketTimeoutException("connect timed out");
        assertFalse(AiProviderClient.isReadTimeout(exception));
    }

    @Test
    void unrelatedExceptionIsNotReadTimeout() {
        assertFalse(AiProviderClient.isReadTimeout(new RuntimeException("boom")));
        assertFalse(AiProviderClient.isReadTimeout(null));
    }
}
