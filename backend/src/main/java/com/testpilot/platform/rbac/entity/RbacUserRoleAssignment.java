package com.testpilot.platform.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_role_assignments")
public class RbacUserRoleAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;

    private String scopeType;

    private Long scopeId;

    private LocalDateTime createdAt;

    protected RbacUserRoleAssignment() {
    }

    public RbacUserRoleAssignment(Long userId, Long roleId, String scopeType, Long scopeId) {
        this.userId = userId;
        this.roleId = roleId;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
