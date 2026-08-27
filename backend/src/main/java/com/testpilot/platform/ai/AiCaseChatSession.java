package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ai_case_chat_sessions")
public class AiCaseChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String suiteType;
    private Long sessionKey;
    private String title;
    private String messagesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AiCaseChatSession() {
    }

    public AiCaseChatSession(Long projectId, Long userId, String suiteType, Long sessionKey, String title, String messagesJson) {
        this.projectId = projectId;
        this.userId = userId;
        this.suiteType = suiteType;
        this.sessionKey = sessionKey;
        this.title = title;
        this.messagesJson = messagesJson;
    }

    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSuiteType() {
        return suiteType;
    }

    public Long getSessionKey() {
        return sessionKey;
    }

    public String getTitle() {
        return title;
    }

    public String getMessagesJson() {
        return messagesJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessagesJson(String messagesJson) {
        this.messagesJson = messagesJson;
    }
}
