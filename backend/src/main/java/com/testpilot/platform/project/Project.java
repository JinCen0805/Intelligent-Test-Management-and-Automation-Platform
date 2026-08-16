package com.testpilot.platform.project;

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
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "default_environment", nullable = false, length = 32)
    private String defaultEnvironment;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Project() {
    }

    public Project(String name, String code, String description, Long ownerId, String defaultEnvironment, String status) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.ownerId = ownerId;
        this.defaultEnvironment = defaultEnvironment;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        deleted = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public Long getOwnerId() { return ownerId; }
    public String getDefaultEnvironment() { return defaultEnvironment; }
    public String getStatus() { return status; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDefaultEnvironment(String defaultEnvironment) { this.defaultEnvironment = defaultEnvironment; }
    public void setStatus(String status) { this.status = status; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
