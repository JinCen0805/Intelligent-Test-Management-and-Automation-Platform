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
@Table(name = "test_tasks")
public class TestTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "automation_project_id", nullable = false)
    private Long automationProjectId;

    @Column(name = "pipeline_binding_id")
    private Long pipelineBindingId;

    @Column(name = "jenkins_job", nullable = false, length = 255)
    private String jenkinsJob;

    @Column(name = "parameters_json", columnDefinition = "text")
    private String parametersJson;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TestTask() {
    }

    public TestTask(
            Long projectId,
            String name,
            String taskType,
            Long automationProjectId,
            String jenkinsJob,
            String parametersJson
    ) {
        this(projectId, name, taskType, automationProjectId, null, jenkinsJob, parametersJson);
    }

    public TestTask(
            Long projectId,
            String name,
            String taskType,
            Long automationProjectId,
            Long pipelineBindingId,
            String jenkinsJob,
            String parametersJson
    ) {
        this.projectId = projectId;
        this.name = name;
        this.taskType = taskType;
        this.automationProjectId = automationProjectId;
        this.pipelineBindingId = pipelineBindingId;
        this.jenkinsJob = jenkinsJob;
        this.parametersJson = parametersJson;
        this.status = "ENABLED";
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

    public void update(
            String name,
            String taskType,
            Long automationProjectId,
            Long pipelineBindingId,
            String jenkinsJob,
            String parametersJson
    ) {
        this.name = name;
        this.taskType = taskType;
        this.automationProjectId = automationProjectId;
        this.pipelineBindingId = pipelineBindingId;
        this.jenkinsJob = jenkinsJob;
        this.parametersJson = parametersJson;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getTaskType() { return taskType; }
    public Long getAutomationProjectId() { return automationProjectId; }
    public Long getPipelineBindingId() { return pipelineBindingId; }
    public String getJenkinsJob() { return jenkinsJob; }
    public String getParametersJson() { return parametersJson; }
    public String getStatus() { return status; }
}
