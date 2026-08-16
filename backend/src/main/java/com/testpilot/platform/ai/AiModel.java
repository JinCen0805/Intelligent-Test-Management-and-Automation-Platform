package com.testpilot.platform.ai;

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
@Table(name = "ai_models")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(nullable = false, length = 500)
    private String endpoint;

    @Column(name = "api_key_ciphertext", columnDefinition = "text")
    private String apiKey;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
    public String getName() { return name; }
    public String getProvider() { return provider; }
    public String getEndpoint() { return endpoint; }
    public String getApiKey() { return apiKey; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
