package com.testpilot.platform.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 生成/分析异步任务。
 *
 * <p>提交时立即创建一条 RUNNING 记录并返回 taskId，后台线程执行真正的 AI 调用与落库，
 * 完成后回写 status / resultJson（或 errorCode / errorMessage），前端轮询该状态。
 */
@Entity
@Table(name = "ai_tasks")
public class AiTask {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_code")
    private Integer errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AiTask() {
    }

    public AiTask(Long projectId, Long createdBy, String type) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.createdBy = createdBy;
        this.type = type;
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
    }

    public void success(String resultJson) {
        this.status = "SUCCESS";
        this.resultJson = resultJson;
        this.errorCode = null;
        this.errorMessage = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(Integer code, String message) {
        this.status = "FAILED";
        this.errorCode = code;
        this.errorMessage = message;
        this.finishedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getCreatedBy() { return createdBy; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getResultJson() { return resultJson; }
    public Integer getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
