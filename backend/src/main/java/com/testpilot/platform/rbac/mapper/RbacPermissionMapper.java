package com.testpilot.platform.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testpilot.platform.rbac.entity.RbacPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RbacPermissionMapper extends BaseMapper<RbacPermission> {
}
