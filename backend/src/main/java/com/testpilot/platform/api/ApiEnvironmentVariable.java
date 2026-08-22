package com.testpilot.platform.api;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("api_environment_variables")
public class ApiEnvironmentVariable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String environment;
    private String name;
    @TableField("value_text")
    private String valueText;
    @TableField("sensitive_flag")
    private boolean sensitive;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ApiEnvironmentVariable() {
    }

    public ApiEnvironmentVariable(Long projectId, String environment, String name, String value, boolean sensitive, String source) {
        this.projectId = projectId;
        this.environment = environment;
        this.name = name;
        this.valueText = value;
        this.sensitive = sensitive;
        this.source = source;
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
    public String getEnvironment() { return environment; }
    public String getName() { return name; }
    public String getValue() { return valueText; }
    public boolean isSensitive() { return sensitive; }
    public String getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void update(String value, boolean sensitive, String source) {
        this.valueText = value;
        this.sensitive = sensitive;
        this.source = source;
    }
}

