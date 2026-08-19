package com.testpilot.platform.api;

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
@Table(name = "api_environment_variables")
public class ApiEnvironmentVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "value_text", columnDefinition = "text")
    private String value;

    @Column(name = "sensitive_flag", nullable = false)
    private boolean sensitive;

    @Column(length = 64)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ApiEnvironmentVariable() {
    }

    public ApiEnvironmentVariable(Long projectId, String environment, String name, String value, boolean sensitive, String source) {
        this.projectId = projectId;
        this.environment = environment;
        this.name = name;
        this.value = value;
        this.sensitive = sensitive;
        this.source = source;
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
    public String getEnvironment() { return environment; }
    public String getName() { return name; }
    public String getValue() { return value; }
    public boolean isSensitive() { return sensitive; }
    public String getSource() { return source; }
    public void update(String value, boolean sensitive, String source) {
        this.value = value;
        this.sensitive = sensitive;
        this.source = source;
    }
}
