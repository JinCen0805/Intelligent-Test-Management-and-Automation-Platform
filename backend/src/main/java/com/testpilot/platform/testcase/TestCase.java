package com.testpilot.platform.testcase;

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
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "case_no", nullable = false, length = 64)
    private String caseNo;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "module_name", length = 128)
    private String moduleName;

    @Column(columnDefinition = "text")
    private String precondition;

    @Column(columnDefinition = "text")
    private String steps;

    @Column(name = "test_data", columnDefinition = "text")
    private String testData;

    @Column(name = "expected_result", columnDefinition = "text")
    private String expectedResult;

    @Column(nullable = false, length = 8)
    private String priority;

    @Column(name = "case_type", nullable = false, length = 32)
    private String caseType;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TestCase() {
    }

    public TestCase(
            Long projectId,
            Long requirementId,
            String caseNo,
            String title,
            String moduleName,
            String precondition,
            String steps,
            String testData,
            String expectedResult,
            String priority,
            String caseType,
            String tags,
            String status,
            String source,
            Long createdBy
    ) {
        this.projectId = projectId;
        this.requirementId = requirementId;
        this.caseNo = caseNo;
        this.title = title;
        this.moduleName = moduleName;
        this.precondition = precondition;
        this.steps = steps;
        this.testData = testData;
        this.expectedResult = expectedResult;
        this.priority = priority;
        this.caseType = caseType;
        this.tags = tags;
        this.status = status;
        this.source = source;
        this.createdBy = createdBy;
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
    public Long getProjectId() { return projectId; }
    public Long getRequirementId() { return requirementId; }
    public String getCaseNo() { return caseNo; }
    public String getTitle() { return title; }
    public String getModuleName() { return moduleName; }
    public String getPrecondition() { return precondition; }
    public String getSteps() { return steps; }
    public String getTestData() { return testData; }
    public String getExpectedResult() { return expectedResult; }
    public String getPriority() { return priority; }
    public String getCaseType() { return caseType; }
    public String getTags() { return tags; }
    public String getStatus() { return status; }
    public String getSource() { return source; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void updateFrom(TestCaseDtos.UpsertRequest request) {
        this.requirementId = request.requirementId();
        this.title = request.title().trim();
        this.moduleName = blankToNull(request.moduleName());
        this.precondition = request.precondition();
        this.steps = request.steps();
        this.testData = request.testData();
        this.expectedResult = request.expectedResult();
        this.priority = request.priority().trim().toUpperCase();
        this.caseType = request.caseType().trim().toUpperCase();
        this.tags = request.tags();
    }

    public void setStatus(String status) { this.status = status; }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
