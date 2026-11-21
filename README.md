# ITMAP — 智能测试管理与自动化平台

以项目为中心管理需求文件、测试用例、接口测试、Jenkins 自动化执行与报告，并接入 AI 做用例生成、用例对话和失败分析。

## 技术栈

- 后端：Spring Boot 3.4 · Java 21 · MyBatis-Plus · Flyway · MySQL · Redis · Spring Security（Bearer + RBAC）
- 前端：Vue 3 · TypeScript · Pinia · Vue Router · Vite
- 集成：Jenkins（Pipeline/Freestyle）、Allure 离线报告、OpenAI 兼容 AI Provider

## 目录结构

```
backend/            Spring Boot 后端（db/migration 为唯一 schema 来源）
frontend/           Vue 3 前端
scripts/            setup-local / start-local / stop-local / verify
database/           历史 schema 参考（已被 Flyway 取代，勿直接修改）
doc/                开发文档、代码审查修复计划与执行报告
deploy/             部署相关脚本
```

## 快速开始

```powershell
# 一次性准备本地配置（MySQL 账号、加密密钥）
scripts\setup-local.ps1

# 一键启动：Redis + 后端(local profile) + 前端（双击 start-local.cmd 也可以）
start-local.cmd

# 打开 http://127.0.0.1:5173/

# 一键停止：前端 + 后端 + 本项目启动的 Redis（双击 stop-local.cmd 也可以）
stop-local.cmd
```

`start-local.cmd` / `stop-local.cmd` 是 cmd 包装器，内部调用 `scripts\start-local.ps1` / `scripts\stop-local.ps1`。
日志写入 `run-local-logs/`。停止脚本只会结束属于本项目的进程；如果 6379 上的 Redis 不是由本项目启动的，会跳过而不是强杀。

演示账号（`local`/`demo`/`test` profile 自动创建）：`lin.xiao / demo123`、`chen.yu / demo123`（管理员）。
其他环境空库首次启动需通过 `ADMIN_INITIAL_USERNAME` / `ADMIN_INITIAL_PASSWORD` 创建初始管理员。

## 验证

```powershell
scripts\verify.cmd              # 后端 mvn test + 前端 build + 前端 Vitest
scripts\verify.cmd -Scope backend
scripts\verify.cmd -Scope frontend
```

CI 配置见 `.github/workflows/ci.yml`，与本地脚本共用同一入口。

## 文档

- [代码审查修复计划](doc/代码审查修复计划.md)（AI 全自动执行计划 + 任务状态表）
- [修复执行报告](doc/修复执行报告.md)（Phase A-D 改动、验证证据与遗留项）
- [技术栈统一改造开发计划](doc/技术栈统一改造开发计划.md)（历史阶段计划）
