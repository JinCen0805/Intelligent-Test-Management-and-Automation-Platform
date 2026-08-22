package com.testpilot.platform.testcase;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("test_cases")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long requirementId;
    private String caseNo;
    private String title;
    private String moduleName;
    private String precondition;
    private String steps;
    private String testData;
    private String expectedResult;
    private String priority;
    private String caseType;
    private String tags;
    private String status;
    private String source;
    private Long createdBy;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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

