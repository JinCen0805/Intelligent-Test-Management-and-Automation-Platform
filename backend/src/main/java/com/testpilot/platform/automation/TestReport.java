package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("test_reports")
public class TestReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private String allureUrl;
    private int totalCount;
    private int passedCount;
    private int failedCount;
    private int brokenCount;
    private int skippedCount;
    private String artifactRef;
    private String failureDetail;
    private String screenshotRef;
    private String videoRef;
    private String aiAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected TestReport() {
    }

    public TestReport(
            Long executionId,
            String allureUrl,
            int totalCount,
            int passedCount,
            int failedCount,
            int brokenCount,
            int skippedCount,
            String artifactRef,
            String failureDetail,
            String screenshotRef,
            String videoRef,
            String aiAnalysis
    ) {
        this.executionId = executionId;
        this.allureUrl = allureUrl;
        this.totalCount = totalCount;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.brokenCount = brokenCount;
        this.skippedCount = skippedCount;
        this.artifactRef = artifactRef;
        this.failureDetail = failureDetail;
        this.screenshotRef = screenshotRef;
        this.videoRef = videoRef;
        this.aiAnalysis = aiAnalysis;
    }
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getExecutionId() { return executionId; }
    public String getAllureUrl() { return allureUrl; }
    public int getTotalCount() { return totalCount; }
    public int getPassedCount() { return passedCount; }
    public int getFailedCount() { return failedCount; }
    public int getBrokenCount() { return brokenCount; }
    public int getSkippedCount() { return skippedCount; }
    public String getArtifactRef() { return artifactRef; }
    public String getFailureDetail() { return failureDetail; }
    public String getScreenshotRef() { return screenshotRef; }
    public String getVideoRef() { return videoRef; }
    public String getAiAnalysis() { return aiAnalysis; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
}

