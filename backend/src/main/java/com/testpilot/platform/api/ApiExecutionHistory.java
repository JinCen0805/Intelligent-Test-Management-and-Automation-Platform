package com.testpilot.platform.api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_execution_history")
public class ApiExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "collection_run_id", length = 64)
    private String collectionRunId;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "response_headers_json", columnDefinition = "text")
    private String responseHeadersJson;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "assertion_results_json", columnDefinition = "text")
    private String assertionResultsJson;

    @Column(name = "request_snapshot_json", columnDefinition = "text")
    private String requestSnapshotJson;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
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
