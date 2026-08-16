# AI 智能测试管理与自动化平台

当前工程已完成前端 MVP 第一版和后端基础 API 第一版，数据库脚本已建立。

## 目录

```text
AITestingPlatform
├── backend                 Spring Boot 后端
├── database                数据库脚本和迁移
├── deploy                  Docker Compose 和部署配置
├── docs                    技术设计和开发计划
├── frontend                Vue 3 前端
├── 原型html                 HTML 原型
└── AI智能测试管理与自动化平台-需求文档-V1.1.md
```

## V1 核心闭环

```text
登录
  ↓
创建项目
  ↓
上传 PRD
  ↓
AI 生成测试用例
  ↓
人工 Review
  ↓
关联 Git 自动化项目
  ↓
Jenkins 执行
  ↓
pytest / Playwright
  ↓
Allure 报告
  ↓
AI 分析失败结果
```

## 当前基线

- 产品需求：`AI智能测试管理与自动化平台-需求文档-V1.1.md`
- 页面原型：`原型html/index.html`
- 技术设计：`docs/`

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 `http://127.0.0.1:5173/`。

当前前端使用 Vue 3 + TypeScript + Vite，已覆盖首页、项目列表、项目详情、需求管理、测试用例、接口测试工作台、自动化项目、测试任务、报告中心和系统管理的 MVP 交互。登录、当前用户和项目列表/创建已接入后端，需求、用例、执行和报告按 `docs/03-后端API规划-V1.md` 继续推进。

## 后端启动

```powershell
cd backend
mvn spring-boot:run
```

默认地址为 `http://127.0.0.1:8080`，使用 H2 本地数据库。接口和 MySQL 配置见 `backend/README.md`。
