# Frontend

Vue 3 + TypeScript + Vite 前端。

## Commands

```powershell
npm install
npm run dev        # 开发服务器 http://127.0.0.1:5173/
npm run build      # vue-tsc 类型检查 + vite 构建
npm run test       # Vitest 单元测试（client / pollAiTask）
```

## Current Scope

- 首页、项目列表与项目详情（文件、用例库、自动化、执行、报告分 Tab）；
- 项目文件上传/解析、AI 用例生成与用例对话、用例审核与导出 Excel；
- 接口测试工作台：集合/请求树、参数/请求头/断言、环境变量、历史记录；
- 自动化项目、Pipeline 绑定、Jenkins Job 导入/创建、测试任务与执行记录（服务端分页）；
- 报告中心、执行详情、离线 Allure 报告与 AI 失败分析；
- 系统管理：用户、RBAC、项目凭证授权、Jenkins 实例、AI 模型与 Prompt。

接口地址由 `.env.development` 的 `VITE_API_BASE_URL` 控制（默认 `/api`，开发代理目标见 `vite.config.ts`）；
真实 Jenkins/AI 配置在后端，本地一键启动脚本使用 MOCK 或真实实例取决于后端配置。

## 验证

在仓库根目录执行统一验证入口（后端测试 + 前端构建 + 前端单测）：

```powershell
scripts\verify.cmd -Scope frontend
# 等价于：
cd frontend; npm run build; npm run test
```

CI 使用 `pwsh scripts/verify.ps1 -Scope frontend`，与本地脚本共用同一入口。
