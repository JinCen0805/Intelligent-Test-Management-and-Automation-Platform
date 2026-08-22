package com.testpilot.platform.project;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("project_members")
public class ProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;

    protected ProjectMember() {
    }

    public ProjectMember(Long projectId, Long userId, String role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setRole(String role) { this.role = role; }
}

