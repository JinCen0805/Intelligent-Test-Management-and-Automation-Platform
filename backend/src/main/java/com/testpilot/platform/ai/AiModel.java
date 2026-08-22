package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_models")
public class AiModel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String provider;
    private String endpoint;
    @TableField("api_key_ciphertext")
    private String apiKey;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AiModel() {
    }

    public AiModel(String name, String provider, String endpoint, String apiKey, boolean enabled) {
        this.name = name;
        this.provider = provider;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
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
    public String getProvider() { return provider; }
    public String getEndpoint() { return endpoint; }
    public String getApiKey() { return apiKey; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void update(String name, String provider, String endpoint, String apiKey, boolean enabled) {
        this.name = name;
        this.provider = provider;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }
}

