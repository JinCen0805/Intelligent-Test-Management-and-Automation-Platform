package com.testpilot.platform.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 通用操作审计记录，覆盖登录、用户变更、项目变更、项目成员变更、用例审批与执行操作。
 *
 * <p>AI 调用由 {@code ai_call_audits} 单独审计，本表负责其余关键操作，供管理员回溯。
 */
@Entity
@Table(name = "operation_audits")
public class OperationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 64)
    private String userName;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(length = 16)
    private String result;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OperationAudit() {
    }

    public OperationAudit(
            Long projectId,
            Long userId,
            String userName,
            String action,
            String targetType,
            Long targetId,
            String detail,
            String result
    ) {
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.result = result;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetail() { return detail; }
    public String getResult() { return result; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
