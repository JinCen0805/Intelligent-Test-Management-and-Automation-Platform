# 阶段3-认证与SessionRedis化验收记录

## 1. 实施日期

2026-08-21

## 2. 已完成内容

- 将登录 Session 从 JVM 内存结构切换为 `AuthSessionStore`；
- Session Key 统一为 `testpilot:*:auth:session:{token}`；
- 增加 `auth_version` 字段，支持用户禁用、改密和角色变更后的旧 token 失效；
- 增加 `RbacPermissionCache`，支持系统权限与项目权限缓存；
- 在用户、项目成员和 RBAC 授权变更时清理权限缓存；
- 保留 Redis 优先、测试环境本地回退的兼容策略；
- 保持现有 `Authorization: Bearer <token>` 请求格式不变。

## 3. 验收测试

测试类：`com.testpilot.platform.Stage3SessionRedisTest`

覆盖：

- 登录后 Session 写入与 key 规则；
- 登出删除 Session；
- 改密后旧 token 失效；
- 项目权限缓存 key 规则；
- RBAC 角色变更后的 authVersion 和缓存失效。

执行命令：

```powershell
cd D:\AITestingPlatform\backend
mvn "-Dtest=Stage3SessionRedisTest" `
  "-Dtestpilot.infrastructure.redis.enabled=true" `
  "-Dtestpilot.infrastructure.redis.required=true" test
```

验收时要求本机 Redis 已在 `127.0.0.1:6379` 监听；本次使用安装目录
`E:\软件安装\Redis-x64-5.0.14.1` 启动的 Redis 5.0.14.1 完成验证。

## 4. 回滚说明

若需回滚，保留 `AuthService`、`AuthSessionStore` 和 `RbacPermissionCache` 代码即可，将 `testpilot.infrastructure.redis.enabled` 设为 `false` 仍可在开发和测试环境使用本地回退模式，不影响现有 API 请求格式。
