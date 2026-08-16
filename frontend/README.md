# Frontend

Vue 3 + TypeScript + Vite 前端 MVP。

## Commands

```powershell
npm install
npm run dev
npm run build
```

开发服务器默认地址：`http://127.0.0.1:5173/`

## Current Scope

- 平台首页和项目列表；
- 项目级需求、测试用例和 Review 入口；
- 接口测试工作台的请求参数、断言、环境变量和模拟响应；
- Git/Jenkins 自动化项目配置展示；
- 测试任务、报告和 AI 失败分析入口；
- 项目创建、需求上传、AI 用例生成和 Jenkins 执行的前端状态流。
- 登录、当前用户和项目列表已接入后端 API；开发环境地址见 `.env.development`。
- 需求列表、详情、Markdown/TXT/PDF/DOCX 上传和解析状态已接入后端 API；
- 测试用例 CRUD、筛选、状态流转、AI 生成、自动化项目/任务、Jenkins 执行和报告已接入后端 API。

本地开发默认使用 MOCK Jenkins 和 MOCK AI；真实环境配置见后端配置文件和项目文档。
