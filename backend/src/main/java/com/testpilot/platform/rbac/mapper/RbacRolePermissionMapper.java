package com.testpilot.platform.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testpilot.platform.rbac.entity.RbacRolePermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RbacRolePermissionMapper extends BaseMapper<RbacRolePermission> {
}
