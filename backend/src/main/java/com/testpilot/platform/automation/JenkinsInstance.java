package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("jenkins_instances")
public class JenkinsInstance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String baseUrl;
    private String username;
    private String apiTokenCiphertext;
    private String status;
    private String connectionStatus;
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private int pollIntervalMs;
    private int maxWaitMinutes;
    private LocalDateTime lastCheckedAt;
    private String lastError;
    private Long createdBy;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

