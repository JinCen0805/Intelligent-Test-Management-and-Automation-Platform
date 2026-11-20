package com.testpilot.platform.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("role_permissions")
public class RbacRolePermission {

    /** 复合主键的一部分：标记为 id 仅为向 MyBatis-Plus 声明主键，避免启动告警。 */
    @TableId(value = "role_id", type = IdType.INPUT)
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
