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
@Table(name = "jenkins_instances")
public class JenkinsInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "api_token_ciphertext", nullable = false, columnDefinition = "text")
    private String apiTokenCiphertext;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "connection_status", nullable = false, length = 16)
    private String connectionStatus;

    @Column(name = "connect_timeout_ms", nullable = false)
    private int connectTimeoutMs;

    @Column(name = "read_timeout_ms", nullable = false)
    private int readTimeoutMs;

    @Column(name = "poll_interval_ms", nullable = false)
    private int pollIntervalMs;

    @Column(name = "max_wait_minutes", nullable = false)
    private int maxWaitMinutes;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JenkinsInstance() {
    }

    public JenkinsInstance(String name, String baseUrl, String username, String apiTokenCiphertext, Long createdBy,
                           int connectTimeoutMs, int readTimeoutMs, int pollIntervalMs, int maxWaitMinutes) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.username = username;
        this.apiTokenCiphertext = apiTokenCiphertext;
        this.createdBy = createdBy;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.pollIntervalMs = pollIntervalMs;
        this.maxWaitMinutes = maxWaitMinutes;
        this.status = "ENABLED";
        this.connectionStatus = "UNKNOWN";
    }

    public void update(String name, String baseUrl, String username, int connectTimeoutMs, int readTimeoutMs,
                       int pollIntervalMs, int maxWaitMinutes) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.username = username;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.pollIntervalMs = pollIntervalMs;
        this.maxWaitMinutes = maxWaitMinutes;
    }

    public void replaceToken(String ciphertext) {
        if (ciphertext != null && !ciphertext.isBlank()) {
            this.apiTokenCiphertext = ciphertext;
        }
    }

    public void markConnection(String status, String error) {
        this.connectionStatus = status;
        this.lastError = error;
        this.lastCheckedAt = LocalDateTime.now();
    }

    public void disable() { this.status = "DISABLED"; }
    public void enable() { this.status = "ENABLED"; }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getUsername() { return username; }
    public String getApiTokenCiphertext() { return apiTokenCiphertext; }
    public String getStatus() { return status; }
    public String getConnectionStatus() { return connectionStatus; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public int getPollIntervalMs() { return pollIntervalMs; }
    public int getMaxWaitMinutes() { return maxWaitMinutes; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public String getLastError() { return lastError; }
    public Long getCreatedBy() { return createdBy; }
}
