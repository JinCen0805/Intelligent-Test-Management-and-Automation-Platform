# Backend

Spring Boot 3.4 + Java 21 后端 MVP。

## 本地启动

```powershell
cd backend
mvn spring-boot:run
```

默认使用本地 H2 文件数据库，服务地址为 `http://127.0.0.1:8080`。

如果本机 `8080` 已被占用，可使用备用端口启动：

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

演示账号：

- 用户名：`lin.xiao`
- 密码：`demo123`

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

MySQL 表结构见 `../database/schema.sql`。生产环境必须替换默认密码，并接入正式的密钥管理与持久化会话方案。
