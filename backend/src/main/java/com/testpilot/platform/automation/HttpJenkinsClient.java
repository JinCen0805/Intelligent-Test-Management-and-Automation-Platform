package com.testpilot.platform.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.testpilot.platform.common.BusinessException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

@Component
public class HttpJenkinsClient implements JenkinsClient {

    private final RestClient restClient;
    private final String mode;
    private final String baseUrl;
    private final String username;
    private final String apiToken;

    public HttpJenkinsClient(
            RestClient.Builder restClientBuilder,
            @Value("${testpilot.jenkins.mode:MOCK}") String mode,
            @Value("${testpilot.jenkins.base-url:}") String baseUrl,
            @Value("${testpilot.jenkins.username:}") String username,
            @Value("${testpilot.jenkins.api-token:}") String apiToken
    ) {
        this.restClient = restClientBuilder.build();
        this.mode = mode;
        this.baseUrl = baseUrl;
        this.username = username;
        this.apiToken = apiToken;
    }

    @Override
    public TriggerResult trigger(AutomationProject automationProject, TestTask task) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            String buildNumber = "MOCK-" + System.currentTimeMillis();
            return new TriggerResult(buildNumber, "mock://jenkins/queue/" + buildNumber);
        }
        ensureConfigured(task.getJenkinsJob());
        String url = baseUrl() + "/" + jobPath(task.getJenkinsJob()) + "/buildWithParameters";
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("BRANCH", task.getBranch());
        parameters.add("ENVIRONMENT", task.getEnvironment());
        if (task.getParametersJson() != null && !task.getParametersJson().isBlank()) {
            parameters.add("PARAMETERS_JSON", task.getParametersJson());
        }
        try {
            var response = restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(this::applyAuth)
                    .body(parameters)
                    .retrieve()
                    .toBodilessEntity();
            String queueUrl = response.getHeaders().getFirst("Location");
            if (queueUrl == null || queueUrl.isBlank()) {
                throw new BusinessException(50210, "Jenkins 未返回 Queue URL");
            }
            return new TriggerResult(null, queueUrl);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(50210, "Jenkins Build 触发失败");
        }
    }

    @Override
    public SyncResult sync(AutomationProject automationProject, TestTask task, TestExecution execution) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            LocalDateTime now = LocalDateTime.now();
            return new SyncResult(
                    execution.getBuildNumber(),
                    execution.getQueueUrl(),
                    "PASSED",
                    "mock-commit",
                    "mock://jenkins/log/" + execution.getId(),
                    "mock://allure/" + execution.getId(),
                    execution.getStartedAt(),
                    now,
                    1,
                    1,
                    0,
                    0,
                    0,
                    "mock://jenkins/artifact/" + execution.getId()
            );
        }

        ensureConfigured(task.getJenkinsJob());
        String buildNumber = execution.getBuildNumber();
        String queueUrl = execution.getQueueUrl();
        String buildUrl = null;
        try {
            if ((buildNumber == null || buildNumber.isBlank()) && queueUrl != null && !queueUrl.isBlank()) {
                JsonNode queue = getJson(queueUrl + (queueUrl.endsWith("/") ? "" : "/") + "api/json");
                JsonNode executable = queue.path("executable");
                if (!executable.isObject() || executable.path("number").isMissingNode()) {
                    return new SyncResult(
                            null,
                            queueUrl,
                            "QUEUED",
                            null,
                            null,
                            null,
                            execution.getStartedAt(),
                            null,
                            0,
                            0,
                            0,
                            0,
                            0,
                            null
                    );
                }
                buildNumber = executable.path("number").asText();
                buildUrl = executable.path("url").asText(null);
            }
            if (buildNumber == null || buildNumber.isBlank()) {
                throw new BusinessException(50211, "Jenkins 未返回 Build Number");
            }
            if (buildUrl == null || buildUrl.isBlank()) {
                buildUrl = baseUrl() + "/" + jobPath(task.getJenkinsJob()) + "/" + buildNumber;
            }
            buildUrl = buildUrl.replaceAll("/+$", "");
            JsonNode build = getJson(buildUrl + "/api/json");
            boolean building = build.path("building").asBoolean(false);
            String result = building ? "RUNNING" : normalizeResult(build.path("result").asText());
            LocalDateTime startedAt = dateTime(build.path("timestamp").asLong(0));
            LocalDateTime finishedAt = building
                    ? null
                    : dateTime(build.path("timestamp").asLong(0) + build.path("duration").asLong(0));
            String commitSha = build.path("changeSet").path("items").path(0).path("commitId").asText(null);
            return new SyncResult(
                    buildNumber,
                    queueUrl,
                    result,
                    commitSha,
                    buildUrl + "/consoleText",
                    buildUrl + "/allure",
                    startedAt,
                    finishedAt,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(50211, "Jenkins Build 状态同步失败");
        }
    }

    private JsonNode getJson(String url) {
        return restClient.get()
                .uri(URI.create(url))
                .headers(this::applyAuth)
                .retrieve()
                .body(JsonNode.class);
    }

    private String normalizeResult(String result) {
        return switch (result) {
            case "SUCCESS" -> "PASSED";
            case "ABORTED" -> "ABORTED";
            case "FAILURE", "UNSTABLE", "NOT_BUILT" -> "FAILED";
            default -> "RUNNING";
        };
    }

    private LocalDateTime dateTime(long epochMillis) {
        if (epochMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()
        );
    }

    private void ensureConfigured(String jenkinsJob) {
        if (baseUrl == null || baseUrl.isBlank() || jenkinsJob == null || jenkinsJob.isBlank()) {
            throw new BusinessException(40008, "Jenkins 地址或 Job 未配置");
        }
    }

    private String baseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }

    private String jobPath(String jenkinsJob) {
        List<String> segments = Arrays.stream(jenkinsJob.split("/"))
                .filter(segment -> !segment.isBlank())
                .map(segment -> "job/" + UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .toList();
        return String.join("/", segments);
    }

    private void applyAuth(org.springframework.http.HttpHeaders headers) {
        if (username != null && !username.isBlank()) {
            headers.setBasicAuth(username, apiToken == null ? "" : apiToken);
        }
    }
}
