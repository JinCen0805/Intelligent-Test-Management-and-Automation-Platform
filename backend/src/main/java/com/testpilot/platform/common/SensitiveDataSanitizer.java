package com.testpilot.platform.common;

import org.springframework.stereotype.Component;

/**
 * 统一的敏感信息脱敏工具，用于在发送日志、测试结果等数据到 AI Provider 之前移除
 * Token、密码、Cookie、认证头与私密/签名 URL 中的凭据。
 *
 * <p>失败分析在读取 Jenkins 日志与失败详情后立即脱敏；AI Provider 客户端在发起请求前
 * 作为最后一道关口对用户输入再次脱敏，保证任何出口都不会带出敏感信息。
 */
@Component
public class SensitiveDataSanitizer {

    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String value = input;

        // 认证头（Authorization / Proxy-Authorization，含 scheme 与凭据）
        value = value.replaceAll(
                "(?i)(\\b(?:authorization|proxy-authorization)\\b\\s*[:=])\\s*\\S+(?:\\s+\\S+)?",
                "$1***"
        );
        // 独立出现的 Bearer token
        value = value.replaceAll("(?i)\\bbearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***");
        // Cookie / Set-Cookie
        value = value.replaceAll("(?i)\\b(set-?cookie|cookie)\\b\\s*[:=]\\s*[^\\r\\n;]+", "$1=***");
        // 敏感键值对（密码、token、secret 等）
        value = value.replaceAll(
                "(?i)\\b(api[_-]?key|apikey|passwd|passphrase|password|secret|client[_-]?secret"
                        + "|access[_-]?token|refresh[_-]?token|token|private[_-]?key)\\b\\s*[:=]\\s*\\S+",
                "$1=***"
        );
        // URL 内嵌凭证 user:pass@
        value = value.replaceAll("(?i)(https?://)[^/@\\s]+:[^/@\\s]+@", "$1***:***@");
        // URL 查询参数中的敏感凭据（私密 / 签名 URL）
        value = value.replaceAll(
                "(?i)([?&](?:access[_-]?token|refresh[_-]?token|token|signature|x-amz-signature"
                        + "|x-amz-credential|x-amz-security-token|api[_-]?key|apikey|secret|password|passwd))"
                        + "=[^&\\s\"']*",
                "$1=***"
        );
        return value;
    }
}
