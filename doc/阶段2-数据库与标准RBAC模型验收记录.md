# 阶段2-数据库与标准RBAC模型验收记录

## 1. 实施日期

2026-08-21

## 2. 已完成内容

- 新增 `V18__rbac_schema.sql`，创建 `roles`、`permissions`、`role_permissions`、`user_role_assignments`；
- 初始化系统角色、项目角色和权限目录；
- 为已有 `users.role`、`projects.owner_id`、`project_members.role` 提供 Flyway 回填；
- 保留旧角色字段，作为阶段5前的兼容读取窗口；
- 新增 MyBatis-Plus RBAC Entity、Mapper 和 `RbacService`；
- 支持系统级权限、项目级权限，以及系统权限对项目权限的继承查询；
- 支持同一用户在不同项目拥有不同项目角色；
- 角色授权与撤销写入 `operation_audits`；
- 非生产环境启动后同步 DemoData 生成的旧角色数据。

## 3. 验收测试

测试类：`com.testpilot.platform.Stage2RbacTest`

覆盖：

- 旧管理员、测试开发用户和项目负责人角色迁移；
- 系统角色与项目角色分层；
- 同一用户跨项目角色隔离；
- 角色到权限的数据库链路可追踪；
- 授权变更审计记录。

执行命令：

```powershell
cd D:\AITestingPlatform\backend
mvn -Dtest=Stage2RbacTest test
```

## 4. 回滚说明

阶段2仅新增 RBAC 表、Mapper 和服务，不删除旧角色字段，也未替换现有业务权限判断。若需回退，可关闭 RBAC 新服务调用并保留 V18 迁移记录，不影响现有业务主表。
