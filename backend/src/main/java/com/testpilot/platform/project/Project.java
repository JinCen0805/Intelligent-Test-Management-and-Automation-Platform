package com.testpilot.platform.project;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("projects")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long ownerId;
    private String status;
    @TableLogic
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Project() {
    }

    public Project(String name, String code, String description, Long ownerId, String status) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.ownerId = ownerId;
        this.status = status;
    }
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        deleted = false;
    }
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public Long getOwnerId() { return ownerId; }
    public String getStatus() { return status; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}

