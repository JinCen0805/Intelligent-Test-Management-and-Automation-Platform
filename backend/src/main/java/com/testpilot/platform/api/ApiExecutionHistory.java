package com.testpilot.platform.api;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("api_execution_history")
public class ApiExecutionHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long requestId;
    private String collectionRunId;
    private String environment;
    private String status;
    private Integer statusCode;
    private Long durationMs;
    private String responseHeadersJson;
    private String responseBody;
    private String assertionResultsJson;
    private String requestSnapshotJson;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    protected ApiExecutionHistory() {
    }

    public ApiExecutionHistory(
            Long projectId,
            Long requestId,
            String collectionRunId,
            String environment,
            String status,
            Integer statusCode,
            Long durationMs,
            String responseHeadersJson,
            String responseBody,
            String assertionResultsJson,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        this(
                projectId, requestId, collectionRunId, environment, status, statusCode, durationMs,
                responseHeadersJson, responseBody, assertionResultsJson, null, startedAt, finishedAt
        );
    }

    public ApiExecutionHistory(
            Long projectId,
            Long requestId,
            String collectionRunId,
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
        this.projectId = projectId;
        this.requestId = requestId;
        this.collectionRunId = collectionRunId;
        this.environment = environment;
        this.status = status;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.responseHeadersJson = responseHeadersJson;
        this.responseBody = responseBody;
        this.assertionResultsJson = assertionResultsJson;
        this.requestSnapshotJson = requestSnapshotJson;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getRequestId() { return requestId; }
    public String getCollectionRunId() { return collectionRunId; }
    public String getEnvironment() { return environment; }
    public String getStatus() { return status; }
    public Integer getStatusCode() { return statusCode; }
    public Long getDurationMs() { return durationMs; }
    public String getResponseHeadersJson() { return responseHeadersJson; }
    public String getResponseBody() { return responseBody; }
    public String getAssertionResultsJson() { return assertionResultsJson; }
    public String getRequestSnapshotJson() { return requestSnapshotJson; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}

