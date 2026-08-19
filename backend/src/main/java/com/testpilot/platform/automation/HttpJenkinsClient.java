package com.testpilot.platform.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.testpilot.platform.common.BusinessException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
            String mode,
            String baseUrl,
            String username,
            String apiToken
    ) {
        this(restClientBuilder, mode, baseUrl, username, apiToken, 5000, 15000);
    }

    @Autowired
    public HttpJenkinsClient(
            RestClient.Builder restClientBuilder,
            @Value("${testpilot.jenkins.mode:MOCK}") String mode,
            @Value("${testpilot.jenkins.base-url:}") String baseUrl,
            @Value("${testpilot.jenkins.username:}") String username,
            @Value("${testpilot.jenkins.api-token:}") String apiToken,
            @Value("${testpilot.jenkins.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${testpilot.jenkins.read-timeout-ms:15000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.mode = mode;
        this.baseUrl = baseUrl;
        this.username = username;
        this.apiToken = apiToken;
    }

    @Override
    public TriggerResult trigger(AutomationProject automationProject, TestTask task) {
        return trigger(automationProject, task, defaultTarget());
    }

    @Override
    public TriggerResult trigger(
            AutomationProject automationProject,
            TestTask task,
            JenkinsTarget target
    ) {
        return trigger(automationProject, task, target, task.getParametersJson());
    }

    @Override
    public TriggerResult trigger(
            AutomationProject automationProject,
            TestTask task,
            JenkinsTarget target,
            String parametersJson
    ) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            String buildNumber = "MOCK-" + System.currentTimeMillis();
            return new TriggerResult(buildNumber, "mock://jenkins/queue/" + buildNumber);
        }
        ensureConfigured(target, task.getJenkinsJob());
        String url = baseUrl(target) + "/" + jobPath(task.getJenkinsJob()) + "/buildWithParameters";
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("BRANCH", automationProject.getBranch());
        parameters.add("ENVIRONMENT", automationProject.getEnvironment());
        parameters.add("REPOSITORY_URL", automationProject.getRepositoryUrl());
        parameters.add("FRAMEWORK", automationProject.getFramework());
        parameters.add("TEST_PATH", automationProject.getTestPath() == null ? "" : automationProject.getTestPath());
        parameters.add("EXECUTE_COMMAND", automationProject.getExecuteCommand() == null ? "" : automationProject.getExecuteCommand());
        parameters.add("CREDENTIAL_REF", automationProject.getCredentialRef() == null ? "" : automationProject.getCredentialRef());
        parameters.add("AUTOMATION_TYPE", automationProject.getAutomationType());
        String effectiveParameters = parametersJson == null || parametersJson.isBlank()
                ? task.getParametersJson()
                : parametersJson;
        if (effectiveParameters != null && !effectiveParameters.isBlank()) {
            parameters.add("PARAMETERS_JSON", effectiveParameters);
        }
        try {
            var response = restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> applyAuth(headers, target))
                    .body(parameters)
                    .retrieve()
                    .toBodilessEntity();
            String queueUrl = response.getHeaders().getFirst("Location");
            if (queueUrl == null || queueUrl.isBlank()) {
                throw new BusinessException(50210, "Jenkins 未返回 Queue URL");
            }
            return new TriggerResult(null, queueUrl);
        } catch (HttpClientErrorException.BadRequest exception) {
            if (exception.getResponseBodyAsString().contains("not parameterized")) {
                return triggerWithoutParameters(baseUrl(target) + "/" + jobPath(task.getJenkinsJob()) + "/build", target);
            }
            throw new BusinessException(50210, "Jenkins Build 触发失败");
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(50210, "Jenkins Build 触发失败");
        }
    }

    private TriggerResult triggerWithoutParameters(String url, JenkinsTarget target) {
        try {
            var response = restClient.post()
                    .uri(URI.create(url))
                    .headers(headers -> applyAuth(headers, target))
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
        return sync(automationProject, task, execution, defaultTarget());
    }

    @Override
    public SyncResult sync(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
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

        ensureConfigured(target, task.getJenkinsJob());
        String buildNumber = execution.getBuildNumber();
        String queueUrl = execution.getQueueUrl();
        String buildUrl = null;
        try {
            if ((buildNumber == null || buildNumber.isBlank()) && queueUrl != null && !queueUrl.isBlank()) {
                JsonNode queue = getJson(queueUrl + (queueUrl.endsWith("/") ? "" : "/") + "api/json", target);
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
                buildUrl = normalizeJenkinsUrl(executable.path("url").asText(null), target);
            }
            if (buildNumber == null || buildNumber.isBlank()) {
                throw new BusinessException(50211, "Jenkins 未返回 Build Number");
            }
            if (buildUrl == null || buildUrl.isBlank()) {
                buildUrl = baseUrl(target) + "/" + jobPath(task.getJenkinsJob()) + "/" + buildNumber;
            }
            buildUrl = buildUrl.replaceAll("/+$", "");
            JsonNode build = getJson(buildUrl + "/api/json", target);
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

    @Override
    public void testConnection(JenkinsTarget target) {
        ensureConfigured(target, "connection");
        if ("MOCK".equalsIgnoreCase(mode)) {
            return;
        }
        try {
            getJson(baseUrl(target) + "/api/json", target);
        } catch (RestClientException exception) {
            throw new BusinessException(50212, "Jenkins 连接测试失败");
        }
    }

    @Override
    public List<JenkinsJob> listJobs(JenkinsTarget target) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            return List.of();
        }
        if (target == null || target.baseUrl() == null || target.baseUrl().isBlank()) {
            throw new BusinessException(40008, "Jenkins 地址未配置");
        }
        try {
            List<JenkinsJob> jobs = new ArrayList<>();
            collectJobs(baseUrl(target) + "/api/json", "", target, jobs, 0);
            return jobs;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(50213, "Jenkins Job 列表读取失败");
        }
    }

    @Override
    public void createJob(JenkinsTarget target, String jobName, String configXml) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            return;
        }
        ensureConfigured(target, jobName);
        String url = baseUrl(target) + "/createItem?name="
                + UriUtils.encodeQueryParam(jobName, StandardCharsets.UTF_8);
        try {
            restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_XML)
                    .headers(headers -> applyAuth(headers, target))
                    .body(configXml)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest exception) {
            String body = exception.getResponseBodyAsString();
            if (body != null && body.toLowerCase().contains("already exists")) {
                throw new BusinessException(40059, "同名 Jenkins Job 已存在");
            }
            throw new BusinessException(50220, "Jenkins Job 创建失败");
        } catch (RestClientException exception) {
            throw new BusinessException(50220, "Jenkins Job 创建失败");
        }
    }

    @Override
    public void cancel(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            return;
        }
        ensureConfigured(target, task.getJenkinsJob());
        String cancelUrl;
        if (execution.getBuildNumber() != null && !execution.getBuildNumber().isBlank()) {
            cancelUrl = baseUrl(target) + "/" + jobPath(task.getJenkinsJob())
                    + "/" + UriUtils.encodePathSegment(
                            execution.getBuildNumber(), StandardCharsets.UTF_8
                    ) + "/stop";
        } else if (execution.getQueueUrl() != null && !execution.getQueueUrl().isBlank()) {
            cancelUrl = execution.getQueueUrl().replaceAll("/+$", "") + "/cancel";
        } else {
            throw new BusinessException(50215, "Jenkins Queue 或 Build 信息缺失，无法取消");
        }
        try {
            restClient.post()
                    .uri(URI.create(normalizeJenkinsUrl(cancelUrl, target)))
                    .headers(headers -> applyAuth(headers, target))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new BusinessException(40456, "Jenkins Queue 或 Build 不存在");
        } catch (RestClientException exception) {
            throw new BusinessException(50216, "Jenkins Build 取消失败");
        }
    }

    @Override
    public byte[] downloadAllureArchive(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        if ("MOCK".equalsIgnoreCase(mode)
                || execution.getBuildNumber() == null
                || execution.getBuildNumber().isBlank()) {
            return null;
        }
        ensureConfigured(target, task.getJenkinsJob());
        String url = baseUrl(target) + "/" + jobPath(task.getJenkinsJob())
                + "/" + UriUtils.encodePathSegment(
                        execution.getBuildNumber(), StandardCharsets.UTF_8
                ) + "/artifact/allure-report.zip";
        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .headers(headers -> applyAuth(headers, target))
                    .retrieve()
                    .body(byte[].class);
        } catch (HttpClientErrorException.NotFound exception) {
            return null;
        } catch (RestClientException exception) {
            throw new BusinessException(50217, "Allure 离线报告下载失败");
        }
    }

    @Override
    public AllureSummary downloadAllureSummary(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        if ("MOCK".equalsIgnoreCase(mode)
                || execution.getBuildNumber() == null
                || execution.getBuildNumber().isBlank()) {
            return null;
        }
        ensureConfigured(target, task.getJenkinsJob());
        String buildBase = baseUrl(target) + "/" + jobPath(task.getJenkinsJob())
                + "/" + UriUtils.encodePathSegment(
                execution.getBuildNumber(), StandardCharsets.UTF_8
        ) + "/artifact/";
        for (String artifact : List.of(
                "allure-summary.json",
                "allure-report/widgets/summary.json",
                "test-summary.json"
        )) {
            try {
                JsonNode root = getJson(buildBase + artifact, target);
                JsonNode summary = root.has("statistic") ? root.path("statistic") : root;
                int total = summary.path("total").asInt(-1);
                int passed = summary.path("passed").asInt(-1);
                int failed = summary.path("failed").asInt(-1);
                int broken = summary.path("broken").asInt(-1);
                int skipped = summary.path("skipped").asInt(-1);
                if (total >= 0 && passed >= 0 && failed >= 0 && broken >= 0 && skipped >= 0
                        && total == passed + failed + broken + skipped
                        + summary.path("unknown").asInt(0)) {
                    return new AllureSummary(total, passed, failed, broken, skipped);
                }
            } catch (HttpClientErrorException.NotFound ignored) {
                // Try the next supported summary artifact format.
            } catch (RestClientException exception) {
                throw new BusinessException(50217, "Allure 统计文件下载失败");
            }
        }
        return null;
    }

    @Override
    public String fetchConsoleLog(JenkinsTarget target, String consoleLogRef) {
        if ("MOCK".equalsIgnoreCase(mode)) {
            return "[INFO] Build started\n"
                    + "[INFO] pip install -r requirements.txt\n"
                    + "[INFO] pytest tests/api --alluredir=allure-results\n"
                    + "[ERROR] selenium.common.exceptions.WebDriverException: Message: 'chromedriver' executable needs to be in PATH\n"
                    + "[ERROR] selenium.common.exceptions.NoSuchElementException: no such element: Unable to locate element\n"
                    + "[INFO] 1 failed, 1 broken\n"
                    + "[INFO] Build finished: FAILURE";
        }
        if (target == null || consoleLogRef == null || consoleLogRef.isBlank()) {
            return null;
        }
        try {
            String url = normalizeJenkinsUrl(consoleLogRef, target);
            return restClient.get()
                    .uri(URI.create(url))
                    .headers(headers -> applyAuth(headers, target))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException exception) {
            throw new BusinessException(50218, "Jenkins 控制台日志读取失败");
        }
    }

    private void collectJobs(
            String apiUrl,
            String parentPath,
            JenkinsTarget target,
            List<JenkinsJob> result,
            int depth
    ) {
        if (depth > 12) {
            throw new BusinessException(50214, "Jenkins Job 嵌套层级超过限制");
        }
        String separator = apiUrl.contains("?") ? "&" : "?";
        String encodedTree = UriUtils.encodeQueryParam(JOB_TREE.substring("tree=".length()), StandardCharsets.UTF_8);
        JsonNode root = getJson(apiUrl + separator + "tree=" + encodedTree, target);
        JsonNode jobs = root.path("jobs");
        if (!jobs.isArray()) {
            return;
        }
        for (JsonNode job : jobs) {
            String name = job.path("name").asText(null);
            if (name == null || name.isBlank()) {
                continue;
            }
            String path = parentPath.isBlank() ? name : parentPath + "/" + name;
            String jobUrl = normalizeJenkinsUrl(job.path("url").asText(null), target);
            String jenkinsClass = job.path("_class").asText("");
            String pipelineType = classifyJob(jenkinsClass);
            if (pipelineType != null) {
                result.add(new JenkinsJob(
                        name,
                        path,
                        jobUrl,
                        pipelineType,
                        jenkinsClass,
                        job.path("buildable").asBoolean(false),
                        parametersJson(job.path("property"))
                ));
                continue;
            }
            if (jobUrl != null && !jobUrl.isBlank()) {
                collectJobs(jobUrl.replaceAll("/+$", "") + "/api/json", path, target, result, depth + 1);
            }
        }
    }

    private String classifyJob(String jenkinsClass) {
        if (jenkinsClass.contains("WorkflowMultiBranchProject")) {
            return "MULTIBRANCH_PIPELINE";
        }
        if (jenkinsClass.contains("WorkflowJob")) {
            return "PIPELINE_JOB";
        }
        if (jenkinsClass.contains("FreeStyleProject")) {
            return "FREESTYLE_JOB";
        }
        return null;
    }

    private String parametersJson(JsonNode properties) {
        if (properties == null || !properties.isArray()) {
            return null;
        }
        for (JsonNode property : properties) {
            JsonNode definitions = property.path("parameterDefinitions");
            if (definitions.isArray() && definitions.size() > 0) {
                return definitions.toString();
            }
        }
        return null;
    }

    private JsonNode getJson(String url, JenkinsTarget target) {
        return restClient.get()
                .uri(URI.create(url))
                .headers(headers -> applyAuth(headers, target))
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

    private void ensureConfigured(JenkinsTarget target, String jenkinsJob) {
        if (target == null || target.baseUrl() == null || target.baseUrl().isBlank()
                || jenkinsJob == null || jenkinsJob.isBlank()) {
            throw new BusinessException(40008, "Jenkins 地址或 Job 未配置");
        }
    }

    private JenkinsTarget defaultTarget() {
        return new JenkinsTarget(baseUrl, username, apiToken, 5000, 15000);
    }

    private String baseUrl(JenkinsTarget target) {
        return target.baseUrl().replaceAll("/+$", "");
    }

    private String normalizeJenkinsUrl(String rawUrl, JenkinsTarget target) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }
        URI rawTarget = URI.create(rawUrl);
        URI configured = URI.create(baseUrl(target));
        String suffix = rawTarget.getRawPath() == null ? "" : rawTarget.getRawPath();
        if (rawTarget.getRawQuery() != null) {
            suffix += "?" + rawTarget.getRawQuery();
        }
        return configured.getScheme() + "://" + configured.getRawAuthority() + suffix;
    }

    private String jobPath(String jenkinsJob) {
        List<String> segments = Arrays.stream(jenkinsJob.split("/"))
                .filter(segment -> !segment.isBlank())
                .map(segment -> "job/" + UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .toList();
        return String.join("/", segments);
    }

    private void applyAuth(org.springframework.http.HttpHeaders headers, JenkinsTarget target) {
        if (target != null && target.username() != null && !target.username().isBlank()) {
            headers.setBasicAuth(target.username(), target.apiToken() == null ? "" : target.apiToken());
        }
    }

    private static final String JOB_TREE =
            "tree=jobs[name,url,_class,buildable,property[parameterDefinitions[name,type,defaultParameterValue[value]]]]";
}
