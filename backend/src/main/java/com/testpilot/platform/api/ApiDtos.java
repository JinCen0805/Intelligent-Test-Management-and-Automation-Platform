package com.testpilot.platform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record CollectionRequest(
            @NotBlank(message = "集合名称不能为空")
            @Size(max = 128, message = "集合名称不能超过 128 个字符")
            String name,
            @Size(max = 500, message = "集合描述不能超过 500 个字符")
            String description
    ) {
    }

    public record CollectionResponse(
            Long id,
            Long projectId,
            String name,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static CollectionResponse from(ApiCollection collection) {
            return new CollectionResponse(
                    collection.getId(),
                    collection.getProjectId(),
                    collection.getName(),
                    collection.getDescription(),
                    collection.getCreatedAt(),
                    collection.getUpdatedAt()
            );
        }
    }

    public record RequestUpsert(
            Long collectionId,
            @NotBlank(message = "请求名称不能为空")
            @Size(max = 128, message = "请求名称不能超过 128 个字符")
            String name,
            @NotBlank(message = "请求方法不能为空")
            String method,
            @NotBlank(message = "请求 URL 不能为空")
            @Size(max = 1000, message = "请求 URL 不能超过 1000 个字符")
            String url,
            String paramsJson,
            String headersJson,
            String body,
            String assertionsJson,
            String preRequestScript,
            String postRequestScript
    ) {
    }

    public record RequestResponse(
            Long id,
            Long projectId,
            Long collectionId,
            String name,
            String method,
            String url,
            String paramsJson,
            String headersJson,
            String body,
            String assertionsJson,
            String preRequestScript,
            String postRequestScript
    ) {
        static RequestResponse from(ApiRequest request) {
            return new RequestResponse(
                    request.getId(),
                    request.getProjectId(),
                    request.getCollectionId(),
                    request.getName(),
                    request.getMethod(),
                    request.getUrl(),
                    request.getParamsJson(),
                    request.getHeadersJson(),
                    request.getBodyText(),
                    request.getAssertionsJson(),
                    request.getPreRequestScript(),
                    request.getPostRequestScript()
            );
        }
    }

    public record VariableUpsert(
            @NotBlank(message = "环境不能为空")
            String environment,
            @NotBlank(message = "变量名不能为空")
            String name,
            String value,
            Boolean sensitive,
            String source
    ) {
    }

    public record VariableResponse(
            Long id,
            String environment,
            String name,
            String value,
            boolean sensitive,
            String source
    ) {
        static VariableResponse from(ApiEnvironmentVariable variable) {
            return new VariableResponse(
                    variable.getId(),
                    variable.getEnvironment(),
                    variable.getName(),
                    variable.isSensitive() ? "••••••••" : variable.getValue(),
                    variable.isSensitive(),
                    variable.getSource()
            );
        }
    }

    public record ExecuteRequest(
            @NotBlank(message = "执行环境不能为空")
            String environment
    ) {
    }

    public record HistoryResponse(
            Long id,
            Long requestId,
            String collectionRunId,
            String requestName,
            String environment,
            String status,
            Integer statusCode,
            Long durationMs,
            String responseHeadersJson,
            String responseBody,
            String assertionResultsJson,
            String requestSnapshotJson,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        static HistoryResponse from(ApiExecutionHistory history, String requestName) {
            return new HistoryResponse(
                    history.getId(),
                    history.getRequestId(),
                    history.getCollectionRunId(),
                    requestName,
                    history.getEnvironment(),
                    history.getStatus(),
                    history.getStatusCode(),
                    history.getDurationMs(),
                    history.getResponseHeadersJson(),
                    history.getResponseBody(),
                    history.getAssertionResultsJson(),
                    history.getRequestSnapshotJson(),
                    history.getStartedAt(),
                    history.getFinishedAt()
            );
        }
    }

    public record HistoryPageResponse(
            java.util.List<HistoryResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
        public static HistoryPageResponse from(
                org.springframework.data.domain.Page<HistoryResponse> page
        ) {
            return new HistoryPageResponse(
                    page.getContent(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.getNumber(),
                    page.getSize()
            );
        }
    }

    public record CollectionExecuteRequest(
            @NotBlank(message = "执行环境不能为空")
            String environment,
            Boolean stopOnFailure
    ) {
    }

    public record CollectionExecutionResponse(
            String runId,
            Long collectionId,
            String environment,
            String status,
            java.util.List<HistoryResponse> results
    ) {
    }
}
