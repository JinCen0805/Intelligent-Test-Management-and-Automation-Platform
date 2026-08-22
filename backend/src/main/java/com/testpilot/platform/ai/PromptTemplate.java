package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("prompt_templates")
public class PromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String taskType;
    private String content;
    private String version;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected PromptTemplate() {
    }

    public PromptTemplate(String name, String taskType, String content, String version, boolean enabled) {
        this.name = name;
        this.taskType = taskType;
        this.content = content;
        this.version = version;
        this.enabled = enabled;
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
    public String getName() { return name; }
    public String getTaskType() { return taskType; }
    public String getContent() { return content; }
    public String getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void update(String name, String taskType, String content, String version, boolean enabled) {
        this.name = name;
        this.taskType = taskType;
        this.content = content;
        this.version = version;
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

