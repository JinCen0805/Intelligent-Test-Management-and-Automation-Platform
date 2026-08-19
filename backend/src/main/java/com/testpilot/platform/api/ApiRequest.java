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
@Table(name = "api_requests")
public class ApiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "params_json", columnDefinition = "text")
    private String paramsJson;

    @Column(name = "headers_json", columnDefinition = "text")
    private String headersJson;

    @Column(name = "body_text", columnDefinition = "text")
    private String bodyText;

    @Column(name = "assertions_json", columnDefinition = "text")
    private String assertionsJson;

    @Column(name = "pre_request_script", columnDefinition = "text")
    private String preRequestScript;

    @Column(name = "post_request_script", columnDefinition = "text")
    private String postRequestScript;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ApiRequest() {
    }

    public ApiRequest(
            Long projectId,
            Long collectionId,
            String name,
            String method,
            String url,
            String paramsJson,
            String headersJson,
            String bodyText,
            String assertionsJson,
            String preRequestScript,
            String postRequestScript
    ) {
        this.projectId = projectId;
        this.collectionId = collectionId;
        this.name = name;
        this.method = method;
        this.url = url;
        this.paramsJson = paramsJson;
        this.headersJson = headersJson;
        this.bodyText = bodyText;
        this.assertionsJson = assertionsJson;
        this.preRequestScript = preRequestScript;
        this.postRequestScript = postRequestScript;
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
    public Long getCollectionId() { return collectionId; }
    public String getName() { return name; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getParamsJson() { return paramsJson; }
    public String getHeadersJson() { return headersJson; }
    public String getBodyText() { return bodyText; }
    public String getAssertionsJson() { return assertionsJson; }
    public String getPreRequestScript() { return preRequestScript; }
    public String getPostRequestScript() { return postRequestScript; }
    public void update(
            Long collectionId,
            String name,
            String method,
            String url,
            String paramsJson,
            String headersJson,
            String bodyText,
            String assertionsJson,
            String preRequestScript,
            String postRequestScript
    ) {
        this.collectionId = collectionId;
        this.name = name;
        this.method = method;
        this.url = url;
        this.paramsJson = paramsJson;
        this.headersJson = headersJson;
        this.bodyText = bodyText;
        this.assertionsJson = assertionsJson;
        this.preRequestScript = preRequestScript;
        this.postRequestScript = postRequestScript;
    }
}
