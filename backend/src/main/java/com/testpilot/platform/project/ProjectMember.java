package com.testpilot.platform.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "joined_at", nullable = false)
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
