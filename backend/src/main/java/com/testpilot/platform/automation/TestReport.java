package com.testpilot.platform.automation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_reports")
public class TestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, unique = true)
    private Long executionId;

    @Column(name = "allure_url", length = 500)
    private String allureUrl;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int passedCount;

    @Column(nullable = false)
    private int failedCount;

    @Column(nullable = false)
    private int brokenCount;

    @Column(nullable = false)
    private int skippedCount;

    @Column(name = "artifact_ref", length = 1000)
    private String artifactRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
            String artifactRef
    ) {
        this.executionId = executionId;
        this.allureUrl = allureUrl;
        this.totalCount = totalCount;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.brokenCount = brokenCount;
        this.skippedCount = skippedCount;
        this.artifactRef = artifactRef;
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

    public Long getId() { return id; }
    public Long getExecutionId() { return executionId; }
    public String getAllureUrl() { return allureUrl; }
    public int getTotalCount() { return totalCount; }
    public int getPassedCount() { return passedCount; }
    public int getFailedCount() { return failedCount; }
    public int getBrokenCount() { return brokenCount; }
    public int getSkippedCount() { return skippedCount; }
    public String getArtifactRef() { return artifactRef; }
}
