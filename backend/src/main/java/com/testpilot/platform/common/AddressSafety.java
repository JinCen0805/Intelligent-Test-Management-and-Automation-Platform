package com.testpilot.platform.common;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 统一的出站地址安全校验，用于拦截 SSRF。
 *
 * <p>所有需要访问用户或管理员配置的目标地址的出口（接口工作台、AI Provider 等）都应复用此校验，
 * 统一拦截本机、回环、内网、链路本地、组播和云元数据地址。
 */
@Component
public class AddressSafety {

    private final Set<String> allowedHosts;

    public AddressSafety(@Value("${testpilot.security.ssrf-allowed-hosts:}") String allowedHosts) {
        this.allowedHosts = allowedHosts == null || allowedHosts.isBlank()
                ? Set.of()
                : Arrays.stream(allowedHosts.split(","))
                        .map(String::trim)
                        .filter(host -> !host.isEmpty())
                        .map(host -> host.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
    }

    public URI validate(String rawUrl) {
        return validate(rawUrl, Set.of());
    }

    public URI validate(String rawUrl, Set<String> extraAllowedHosts) {
        final URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(40037, "请求 URL 不合法");
        }
        if (!Set.of("http", "https").contains(uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(40038, "仅允许使用 HTTP 或 HTTPS 协议");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new BusinessException(40037, "请求 URL 不合法");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (allowedHosts.contains(host) || extraAllowedHosts.contains(host)) {
            return uri;
        }
        if (host.equals("localhost")
                || host.endsWith(".localhost")
                || host.equals("metadata.google.internal")
                || host.equals("169.254.169.254")
                || host.equals("metadata.internal")) {
            throw new BusinessException(40039, "禁止访问本机、内网或云元数据地址");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isPrivateAddress(address)) {
                    throw new BusinessException(40039, "禁止访问本机、内网或云元数据地址");
                }
            }
        } catch (UnknownHostException exception) {
            throw new BusinessException(40040, "目标域名无法解析");
        }
        return uri;
    }

    private boolean isPrivateAddress(InetAddress address) {
        String hostAddress = address.getHostAddress().toLowerCase(Locale.ROOT);
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || hostAddress.startsWith("fc")
                || hostAddress.startsWith("fd")
                || hostAddress.startsWith("0:0:0:0:0:ffff:127.")
                || hostAddress.startsWith("0:0:0:0:0:ffff:10.")
                || hostAddress.startsWith("0:0:0:0:0:ffff:192.168.");
    }
}
