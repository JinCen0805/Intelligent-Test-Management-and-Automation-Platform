package com.testpilot.platform.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.AddressSafety;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiWorkbenchService {

    private final ApiCollectionRepository collectionRepository;
    private final ApiRequestRepository requestRepository;
    private final ApiEnvironmentVariableRepository variableRepository;
    private final ApiExecutionHistoryRepository historyRepository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AddressSafety addressSafety;

    public ApiWorkbenchService(
            ApiCollectionRepository collectionRepository,
            ApiRequestRepository requestRepository,
            ApiEnvironmentVariableRepository variableRepository,
            ApiExecutionHistoryRepository historyRepository,
            ProjectAccessService projectAccessService,
            ObjectMapper objectMapper,
            AddressSafety addressSafety
    ) {
        this.collectionRepository = collectionRepository;
        this.requestRepository = requestRepository;
        this.variableRepository = variableRepository;
        this.historyRepository = historyRepository;
        this.projectAccessService = projectAccessService;
        this.objectMapper = objectMapper;
        this.addressSafety = addressSafety;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public List<ApiDtos.CollectionResponse> listCollections(Long projectId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        return collectionRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)
                .stream().map(ApiDtos.CollectionResponse::from).toList();
    }

    @Transactional
    public ApiDtos.CollectionResponse createCollection(Long projectId, ApiDtos.CollectionRequest request, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        return ApiDtos.CollectionResponse.from(collectionRepository.save(
                new ApiCollection(projectId, request.name().trim(), blankToNull(request.description()))
        ));
    }

    public List<ApiDtos.RequestResponse> listRequests(Long projectId, Long collectionId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        requireCollection(projectId, collectionId);
        return requestRepository.findAllByCollectionIdOrderByCreatedAtAsc(collectionId)
                .stream().map(ApiDtos.RequestResponse::from).toList();
    }

    @Transactional
    public void deleteCollection(Long projectId, Long collectionId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        requireCollection(projectId, collectionId);
        List<ApiRequest> requests = requestRepository.findAllByCollectionIdOrderByCreatedAtAsc(collectionId);
        requests.forEach(request -> historyRepository.deleteAll(
                historyRepository.findAllByRequestId(request.getId())
        ));
        requestRepository.deleteAll(requests);
        collectionRepository.deleteById(collectionId);
    }

    @Transactional
    public void deleteRequest(Long projectId, Long requestId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        ApiRequest request = requestRepository.findByIdAndProjectId(requestId, projectId)
                .orElseThrow(() -> new BusinessException(40431, "接口请求不存在"));
        historyRepository.deleteAll(historyRepository.findAllByRequestId(requestId));
        requestRepository.delete(request);
    }

    @Transactional
    public ApiDtos.RequestResponse saveRequest(
            Long projectId,
            ApiDtos.RequestUpsert request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        requireCollection(projectId, request.collectionId());
        String method = request.method().trim().toUpperCase(Locale.ROOT);
        validateMethod(method);
        ApiRequest saved = requestRepository.save(new ApiRequest(
                projectId,
                request.collectionId(),
                request.name().trim(),
                method,
                request.url().trim(),
                normalizeJson(request.paramsJson(), "{}"),
                normalizeJson(request.headersJson(), "{}"),
                request.body(),
                normalizeJson(request.assertionsJson(), "[]"),
                blankToNull(request.preRequestScript()),
                blankToNull(request.postRequestScript())
        ));
        return ApiDtos.RequestResponse.from(saved);
    }

    @Transactional
    public ApiDtos.RequestResponse updateRequest(
            Long projectId,
            Long requestId,
            ApiDtos.RequestUpsert request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        ApiRequest saved = requestRepository.findByIdAndProjectId(requestId, projectId)
                .orElseThrow(() -> new BusinessException(40431, "接口请求不存在"));
        requireCollection(projectId, request.collectionId());
        String method = request.method().trim().toUpperCase(Locale.ROOT);
        validateMethod(method);
        saved.update(
                request.collectionId(),
                request.name().trim(),
                method,
                request.url().trim(),
                normalizeJson(request.paramsJson(), "{}"),
                normalizeJson(request.headersJson(), "{}"),
                request.body(),
                normalizeJson(request.assertionsJson(), "[]"),
                blankToNull(request.preRequestScript()),
                blankToNull(request.postRequestScript())
        );
        return ApiDtos.RequestResponse.from(requestRepository.save(saved));
    }

    public List<ApiDtos.VariableResponse> listVariables(Long projectId, String environment, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        String normalized = normalizeEnvironment(environment);
        return variableRepository.findAllByProjectIdAndEnvironmentOrderByNameAsc(projectId, normalized)
                .stream().map(ApiDtos.VariableResponse::from).toList();
    }

    @Transactional
    public ApiDtos.VariableResponse saveVariable(
            Long projectId,
            ApiDtos.VariableUpsert request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        String environment = normalizeEnvironment(request.environment());
        ApiEnvironmentVariable variable = variableRepository
                .findByProjectIdAndEnvironmentAndName(projectId, environment, request.name().trim())
                .orElseGet(() -> new ApiEnvironmentVariable(
                        projectId,
                        environment,
                        request.name().trim(),
                        request.value(),
                        Boolean.TRUE.equals(request.sensitive()),
                        blankToNull(request.source())
                ));
        if (variable.getId() != null) {
            variable.update(request.value(), Boolean.TRUE.equals(request.sensitive()), blankToNull(request.source()));
        }
        return ApiDtos.VariableResponse.from(variableRepository.save(variable));
    }

    @Transactional
    public void deleteVariable(Long projectId, Long variableId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        ApiEnvironmentVariable variable = variableRepository.findByIdAndProjectId(variableId, projectId)
                .orElseThrow(() -> new BusinessException(40433, "环境变量不存在"));
        variableRepository.delete(variable);
    }

    public Page<ApiDtos.HistoryResponse> listHistory(Long projectId, int page, int size, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        Map<Long, String> requestNames = requestRepository.findAllByProjectId(projectId).stream()
                .collect(java.util.stream.Collectors.toMap(ApiRequest::getId, ApiRequest::getName));
        return historyRepository.findAllByProjectIdOrderByStartedAtDesc(
                        projectId,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))
                )
                .map(history -> ApiDtos.HistoryResponse.from(
                        history, requestNames.getOrDefault(history.getRequestId(), "已删除请求")
                ));
    }

    @Transactional
    public ApiDtos.HistoryResponse execute(
            Long projectId,
            Long requestId,
            ApiDtos.ExecuteRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        ApiRequest apiRequest = requestRepository.findByIdAndProjectId(requestId, projectId)
                .orElseThrow(() -> new BusinessException(40431, "接口请求不存在"));
        return executeRequest(projectId, apiRequest, normalizeEnvironment(request.environment()), null, new HashMap<>(), null);
    }

    @Transactional
    public ApiDtos.CollectionExecutionResponse executeCollection(
            Long projectId,
            Long collectionId,
            ApiDtos.CollectionExecuteRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.API_EXECUTE);
        requireCollection(projectId, collectionId);
        String environment = normalizeEnvironment(request.environment());
        String runId = java.util.UUID.randomUUID().toString();
        Map<String, String> context = new HashMap<>();
        Map<String, String> environmentVariables = variableRepository
                .findAllByProjectIdAndEnvironmentOrderByNameAsc(projectId, environment)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ApiEnvironmentVariable::getName, ApiEnvironmentVariable::getValue));
        List<ApiDtos.HistoryResponse> results = new ArrayList<>();
        String status = "PASSED";
        for (ApiRequest apiRequest : requestRepository.findAllByCollectionIdOrderByCreatedAtAsc(collectionId)) {
            ApiDtos.HistoryResponse result = executeRequest(
                    projectId, apiRequest, environment, runId, context, environmentVariables);
            results.add(result);
            if (!"PASSED".equals(result.status())) {
                status = "FAILED";
                if (Boolean.TRUE.equals(request.stopOnFailure())) {
                    break;
                }
            }
        }
        return new ApiDtos.CollectionExecutionResponse(runId, collectionId, environment, status, results);
    }

    private ApiDtos.HistoryResponse executeRequest(
            Long projectId,
            ApiRequest apiRequest,
            String environment,
            String collectionRunId,
            Map<String, String> context,
            Map<String, String> preloadedVariables
    ) {
        LocalDateTime startedAt = LocalDateTime.now();
        long start = System.nanoTime();
        int statusCode = 0;
        String responseHeaders = "{}";
        String responseBody = "";
        String status = "FAILED";
        String assertionResults = "[]";
        try {
            Map<String, String> variables = new HashMap<>(preloadedVariables == null
                    ? variableRepository.findAllByProjectIdAndEnvironmentOrderByNameAsc(projectId, environment)
                        .stream().collect(java.util.stream.Collectors.toMap(
                                ApiEnvironmentVariable::getName, ApiEnvironmentVariable::getValue))
                    : preloadedVariables);
            variables.putAll(context);
            runScript(apiRequest.getPreRequestScript(), variables);
            String url = resolve(apiRequest.getUrl(), variables);
            JsonNode params = objectMapper.readTree(normalizeJson(apiRequest.getParamsJson(), "{}"));
            if (params.isObject()) {
                List<String> query = new ArrayList<>();
                params.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isNull() && !entry.getValue().asText().isBlank()) {
                        query.add(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8)
                                + "=" + java.net.URLEncoder.encode(resolve(entry.getValue().asText(), variables), java.nio.charset.StandardCharsets.UTF_8));
                    }
                });
                if (!query.isEmpty()) {
                    url += (url.contains("?") ? "&" : "?") + String.join("&", query);
                }
            }
            URI target = validateTarget(url);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(target)
                    .timeout(Duration.ofSeconds(30));
            JsonNode headers = objectMapper.readTree(normalizeJson(apiRequest.getHeadersJson(), "{}"));
            if (headers.isObject()) {
                headers.fields().forEachRemaining(entry -> builder.header(
                        entry.getKey(),
                        resolve(entry.getValue().asText(), variables)
                ));
            }
            String body = apiRequest.getBodyText() == null ? "" : resolve(apiRequest.getBodyText(), variables);
            HttpRequest.BodyPublisher publisher = body.isBlank()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);
            builder.method(apiRequest.getMethod(), publisher);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode();
            responseBody = response.body();
            responseHeaders = objectMapper.writeValueAsString(response.headers().map());
            assertionResults = evaluateAssertions(
                    apiRequest.getAssertionsJson(),
                    response.statusCode(),
                    response.body(),
                    responseHeaders
            );
            boolean assertionsPassed = objectMapper.readTree(assertionResults).findValuesAsText("passed")
                    .stream().allMatch("true"::equals);
            runScript(apiRequest.getPostRequestScript(), variables, response.body());
            context.putAll(variables);
            status = response.statusCode() >= 200 && response.statusCode() < 400 && assertionsPassed
                    ? "PASSED" : "FAILED";
        } catch (Exception exception) {
            responseBody = exception.getMessage() == null ? "请求执行失败" : exception.getMessage();
        }
        LocalDateTime finishedAt = LocalDateTime.now();
        ApiExecutionHistory history = historyRepository.save(new ApiExecutionHistory(
                projectId,
                apiRequest.getId(),
                collectionRunId,
                environment,
                status,
                statusCode == 0 ? null : statusCode,
                (System.nanoTime() - start) / 1_000_000,
                responseHeaders,
                responseBody,
                assertionResults,
                snapshot(apiRequest),
                startedAt,
                finishedAt
        ));
        return ApiDtos.HistoryResponse.from(history, apiRequest.getName());
    }

    private String evaluateAssertions(
            String assertionsJson,
            int statusCode,
            String responseBody,
            String responseHeadersJson
    ) throws Exception {
        JsonNode assertions = objectMapper.readTree(normalizeJson(assertionsJson, "[]"));
        List<Map<String, Object>> results = new ArrayList<>();
        if (assertions.isArray()) {
            for (JsonNode assertion : assertions) {
                if (!assertion.path("enabled").asBoolean(true)) {
                    continue;
                }
                String type = assertion.path("type").asText("status").toLowerCase(Locale.ROOT);
                String expected = assertion.path("expected").asText("");
                boolean passed;
                if ("status".equals(type)) {
                    passed = String.valueOf(statusCode).equals(expected);
                } else if ("field".equals(type)) {
                    JsonNode node = objectMapper.readTree(responseBody);
                    JsonNode field = node.at(toJsonPointer(assertion.path("path").asText("")));
                    String fieldType = assertion.path("valueType").asText("");
                    boolean exists = !field.isMissingNode();
                    boolean expectedExists = assertion.path("exists").asBoolean(true);
                    boolean typeMatches = fieldType.isBlank()
                            || (exists && field.getNodeType().name().equalsIgnoreCase(fieldType));
                    boolean valueMatches = expected.isBlank()
                            || (exists && expected.equals(field.asText()));
                    passed = expectedExists ? exists && typeMatches && valueMatches : !exists;
                } else if ("contains".equals(type)) {
                    passed = responseBody.contains(expected);
                } else if ("header".equals(type)) {
                    passed = expected.equalsIgnoreCase(readHeader(responseHeadersJson, assertion.path("name").asText("")));
                } else {
                    String actual = readJsonPath(responseBody, assertion.path("path").asText(""));
                    passed = "equals".equalsIgnoreCase(assertion.path("operator").asText("equals"))
                            ? actual.equals(expected)
                            : actual.contains(expected);
                }
                results.add(Map.of(
                        "type", type,
                        "path", assertion.path("path").asText(""),
                        "expected", expected,
                        "passed", passed
                ));
            }
        }
        return objectMapper.writeValueAsString(results);
    }

    private String readJsonPath(String responseBody, String path) throws Exception {
        JsonNode node = objectMapper.readTree(responseBody);
        JsonNode value = node.at(toJsonPointer(path));
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private String toJsonPointer(String path) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.startsWith("$")) {
            normalized = normalized.substring(1);
        }
        // Support array indices $..items[0].. and bracket keys $..['name'] / ["name"]
        normalized = normalized.replaceAll("\\[\\s*(\\d+)\\s*]", "/$1");
        normalized = normalized.replaceAll("\\[\\s*['\"]([^'\"]+)['\"]\\s*]", "/$1");
        normalized = normalized.replace(".", "/");
        normalized = normalized.replaceAll("/+", "/");
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String readHeader(String headersJson, String name) throws Exception {
        JsonNode headers = objectMapper.readTree(headersJson == null ? "{}" : headersJson);
        for (var fields = headers.fields(); fields.hasNext();) {
            var entry = fields.next();
            if (entry.getKey().equalsIgnoreCase(name) && entry.getValue().isArray() && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0).asText();
            }
        }
        return "";
    }

    private String resolve(String value, Map<String, String> variables) {
        String resolved = value;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return resolved;
    }

    private void runScript(String script, Map<String, String> variables) {
        runScript(script, variables, null);
    }

    private void runScript(String script, Map<String, String> variables, String responseBody) {
        if (script == null || script.isBlank()) {
            return;
        }
        for (String rawLine : script.split("\\R|;")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("set ")) {
                String assignment = line.substring(4);
                int separator = assignment.indexOf('=');
                if (separator <= 0) {
                    throw new BusinessException(40034, "脚本 set 语句格式不正确");
                }
                variables.put(assignment.substring(0, separator).trim(), assignment.substring(separator + 1).trim());
            } else if (line.startsWith("extract ")) {
                if (responseBody == null) {
                    throw new BusinessException(40035, "extract 只能用于后置脚本");
                }
                String assignment = line.substring(8);
                int separator = assignment.indexOf('=');
                if (separator <= 0) {
                    throw new BusinessException(40035, "脚本 extract 语句格式不正确");
                }
                try {
                    String value = readJsonPath(
                            responseBody,
                            assignment.substring(separator + 1).trim()
                    );
                    variables.put(assignment.substring(0, separator).trim(), value);
                } catch (Exception exception) {
                    throw new BusinessException(40035, "脚本 extract JSON 路径不正确");
                }
            } else {
                throw new BusinessException(40036, "脚本只支持 set name=value 和 extract name=$.path");
            }
        }
    }

    private URI validateTarget(String rawUrl) {
        return addressSafety.validate(rawUrl);
    }

    private void validateMethod(String method) {
        if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD").contains(method)) {
            throw new BusinessException(40031, "请求方法不支持");
        }
    }

    private ApiCollection requireCollection(Long projectId, Long collectionId) {
        if (collectionId == null) {
            throw new BusinessException(40032, "必须选择请求集合");
        }
        return collectionRepository.findByIdAndProjectId(collectionId, projectId)
                .orElseThrow(() -> new BusinessException(40430, "接口集合不存在"));
    }

    private String normalizeJson(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            objectMapper.readTree(value);
            return value;
        } catch (Exception exception) {
            throw new BusinessException(40033, "接口配置 JSON 格式不正确");
        }
    }

    private String normalizeEnvironment(String environment) {
        return environment == null || environment.isBlank()
                ? "TEST"
                : environment.trim().toUpperCase(Locale.ROOT);
    }

    private String snapshot(ApiRequest request) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "name", request.getName(),
                    "method", request.getMethod(),
                    "url", request.getUrl(),
                    "paramsJson", request.getParamsJson(),
                    "headersJson", request.getHeadersJson(),
                    "body", request.getBodyText() == null ? "" : request.getBodyText(),
                    "assertionsJson", request.getAssertionsJson()
            ));
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
