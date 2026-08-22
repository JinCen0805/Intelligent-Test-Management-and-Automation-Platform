package com.testpilot.platform.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("role_permissions")
public class RbacRolePermission {

    private Long roleId;

    private Long permissionId;

    protected RbacRolePermission() {
    }

    public RbacRolePermission(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
