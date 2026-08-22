package com.testpilot.platform.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("permissions")
public class RbacPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String resource;

    private String action;

    private Boolean enabled;

    protected RbacPermission() {
    }

    public RbacPermission(String code, String name, String resource, String action, Boolean enabled) {
        this.code = code;
        this.name = name;
        this.resource = resource;
        this.action = action;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
