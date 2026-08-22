package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("test_tasks")
public class TestTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String taskType;
    private Long automationProjectId;
    private Long pipelineBindingId;
    private String jenkinsJob;
    private String environment;
    private String branch;
    private String parametersJson;
    private String status;
    private LocalDateTime createdAt;
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
        this(projectId, name, taskType, automationProjectId, null, null, jenkinsJob, parametersJson);
    }

    public TestTask(
            Long projectId,
            String name,
            String taskType,
            Long automationProjectId,
            String environment,
            String branch,
            String jenkinsJob,
            String parametersJson
    ) {
        this(projectId, name, taskType, automationProjectId, null, environment, branch, jenkinsJob, parametersJson);
    }

    public TestTask(
            Long projectId,
            String name,
            String taskType,
            Long automationProjectId,
            Long pipelineBindingId,
            String environment,
            String branch,
            String jenkinsJob,
            String parametersJson
    ) {
        this.projectId = projectId;
        this.name = name;
        this.taskType = taskType;
        this.automationProjectId = automationProjectId;
        this.pipelineBindingId = pipelineBindingId;
        this.environment = environment;
        this.branch = branch;
        this.jenkinsJob = jenkinsJob;
        this.parametersJson = parametersJson;
        this.status = "ENABLED";
    }
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(
            String name,
            String taskType,
            Long automationProjectId,
            Long pipelineBindingId,
            String environment,
            String branch,
            String jenkinsJob,
            String parametersJson
    ) {
        this.name = name;
        this.taskType = taskType;
        this.automationProjectId = automationProjectId;
        this.pipelineBindingId = pipelineBindingId;
        this.environment = environment;
        this.branch = branch;
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
    public String getEnvironment() { return environment; }
    public String getBranch() { return branch; }
    public String getParametersJson() { return parametersJson; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

