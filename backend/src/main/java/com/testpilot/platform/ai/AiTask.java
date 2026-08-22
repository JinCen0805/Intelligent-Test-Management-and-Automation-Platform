package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 生成/分析异步任务。
 *
 * <p>提交时立即创建一条 RUNNING 记录并返回 taskId，后台线程执行真正的 AI 调用与落库，
 * 完成后回写 status / resultJson（或 errorCode / errorMessage），前端轮询该状态。
 */
@TableName("ai_tasks")
public class AiTask {

    @TableId(type = IdType.INPUT)
    private String id;
    private Long projectId;
    private Long createdBy;
    private String type;
    private String status;
    private String resultJson;
    private Integer errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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

