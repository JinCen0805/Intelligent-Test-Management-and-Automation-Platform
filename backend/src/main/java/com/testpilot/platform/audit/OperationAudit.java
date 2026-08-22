package com.testpilot.platform.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 通用操作审计记录，覆盖登录、用户变更、项目变更、项目成员变更、用例审批与执行操作。
 *
 * <p>AI 调用由 {@code ai_call_audits} 单独审计，本表负责其余关键操作，供管理员回溯。
 */
@TableName("operation_audits")
public class OperationAudit {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String userName;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String result;
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

