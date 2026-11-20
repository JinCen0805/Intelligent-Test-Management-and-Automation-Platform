# Backend

Spring Boot 3.4 + Java 21 后端 MVP。

## 本地启动

推荐在仓库根目录运行一键脚本（会依次启动 Redis、后端、前端，并激活 `local` profile）：

```powershell
scripts\start-local.cmd
```

手动启动后端（MySQL，默认端口 8080）：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

`local` / `demo` / `test` profile 会创建演示数据，演示账号：

- 用户名：`lin.xiao`，密码：`demo123`
- 用户名：`chen.yu`，密码：`demo123`（管理员）

其他 profile 首次启动且数据库为空时，必须配置初始管理员，否则启动失败并给出提示：

```powershell
$env:ADMIN_INITIAL_USERNAME = "admin"
$env:ADMIN_INITIAL_PASSWORD = "至少 8 位"
```

MySQL 连接见下一节。如果 8080 已被占用，可用 `--server.port=18080` 覆盖，并同步调整 `frontend/vite.config.ts` 的代理目标。

## 已实现接口

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`
- `GET /api/projects/{projectId}/requirements`
- `POST /api/projects/{projectId}/requirements/upload`
- `POST /api/projects/{projectId}/requirements/{requirementId}/parse`

登录返回的 `accessToken` 通过 `Authorization: Bearer <token>` 访问需要登录的接口。

## MySQL

使用 `application-mysql` profile，并通过环境变量提供连接信息：

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/testpilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "testpilot"
$env:DB_PASSWORD = "change-me"
mvn spring-boot:run
```

MySQL 表结构由 Flyway 迁移维护（`src/main/resources/db/migration`），`../database/schema.sql` 仅为历史参考。

## 验证

在仓库根目录执行统一验证入口（后端测试 + 前端构建）：

```powershell
scripts\verify.cmd
```

只验证后端：

```powershell
scripts\verify.cmd -Scope backend
# 等价于：
cd backend; mvn -B test
```

CI 配置见 `.github/workflows/ci.yml`，与本地脚本共用同一入口（Linux 下直接 `pwsh scripts/verify.ps1`）。
