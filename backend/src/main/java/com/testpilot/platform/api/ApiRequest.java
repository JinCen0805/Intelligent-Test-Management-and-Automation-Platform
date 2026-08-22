package com.testpilot.platform.api;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("api_requests")
public class ApiRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long collectionId;
    private String name;
    private String method;
    private String url;
    private String paramsJson;
    private String headersJson;
    private String bodyText;
    private String assertionsJson;
    private String preRequestScript;
    private String postRequestScript;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
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

