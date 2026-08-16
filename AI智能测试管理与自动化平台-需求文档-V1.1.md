# AI 智能测试管理与自动化平台

## 需求文档 V1.2

**文档状态：** 产品需求基线  
**产品定位：** 一个以项目为中心、AI 增强的企业级测试管理与自动化执行平台。  
**适用角色：** 管理员、测试开发  
**核心原则：** 少而完整，而不是多而复杂。

**V1 核心业务闭环：**

```text
创建项目
  ↓
上传 PRD
  ↓
AI 生成测试用例
  ↓
人工 Review
  ↓
测试开发工程师在本地 IDE 开发自动化代码
  ↓
Git Repository
  ↓
平台关联自动化项目
  ↓
Jenkins
  ↓
pytest / Playwright
  ↓
Allure
  ↓
平台查看测试报告
  ↓
AI 分析失败结果
  ↓
测试开发工程师修复代码并再次执行
```

---

## 1. 产品目标

### 1.1 总体目标

让测试开发工程师围绕一个业务项目，在同一平台完成：

- 上传和查看 PRD；
- 使用 AI 生成测试用例；
- 人工 Review 并维护测试用例；
- 管理多个接口自动化和 UI 自动化项目；
- 关联 Git Repository、Branch、测试路径和执行命令；
- 配置 Jenkins Job 和测试环境；
- 一键触发自动化测试；
- 查看 Jenkins 构建状态和日志；
- 查看测试结果、Allure 报告、截图和视频；
- 使用 AI 分析测试失败原因并获得修复建议。

### 1.2 产品价值

产品以以下结果衡量价值：

- 减少测试用例编写时间；
- 让 AI 生成结果经过 Review 后可沉淀为正式测试用例；
- 让已有自动化代码可以通过平台统一执行；
- 让执行结果、报告和失败分析在项目上下文中可追溯。

### 1.3 产品原则

- Git 是自动化代码的 Source of Truth；
- 自动化代码由测试开发工程师在本地 IDE 开发维护，平台负责管理和编排；
- Jenkins 负责 CI/CD 和执行调度；
- Jenkins Agent 或 Server 负责实际测试执行；
- Allure 负责测试报告生成基础；
- AI 是增强层，不替代测试开发工程师；
- 自动化代码由 Jenkins Agent 或 Server 执行；
- 平台管理数据与自动化代码解耦；
- Secret 不得以明文进入数据库、Git、命令行参数或测试日志。

---

## 2. 产品边界

### 2.1 产品能力

- 登录；
- 用户和项目管理；
- 需求文档上传和查看；
- AI 生成测试用例；
- 测试用例 Draft、Review 和正式管理；
- 接口自动化项目管理；
- 接口测试工作台；
- UI 自动化项目管理；
- Git Repository 关联；
- Jenkins 配置和执行；
- 简单测试任务；
- 测试执行历史；
- Jenkins 日志和测试结果；
- Allure 报告展示；
- Screenshot、Video、Error 信息展示；
- AI 测试失败分析；
- 简单项目环境配置；
- 管理员、测试开发两类角色。

## 3. 用户角色与权限

V1 只保留两个角色。

### 3.1 管理员

- 用户新增、编辑、启用和禁用；
- 创建、编辑和删除项目；
- 管理项目成员；
- 配置 AI 模型；
- 配置 Prompt 模板和测试规则；
- 配置 Jenkins URL 和 Credentials；
- 配置 Git Credentials；
- 查看系统级配置。

### 3.2 测试开发

- 查看参与的项目；
- 上传和管理需求文档；
- 使用 AI 生成测试用例；
- Review 和维护测试用例；
- 管理接口自动化项目；
- 使用接口测试工作台进行接口调试、集合管理和快速验证；
- 管理 UI 自动化项目；
- 关联 Git Repository；
- 配置测试任务；
- 触发 Jenkins；
- 查看执行状态、日志和报告；
- 使用 AI 分析测试失败。

### 3.3 权限原则

平台采用简单的项目级角色权限：

- 管理员可以访问平台管理能力；
- 测试开发只能访问参与的项目；
- 角色为管理员和测试开发；
- 管理员管理平台配置，测试开发管理参与项目的测试资产和执行任务。

---

## 4. 系统信息架构

### 4.1 一级导航

```text
首页
项目
AI 能力
测试报告
系统管理
```

“AI 能力”用于模型和 Prompt 模板的必要配置，但 AI 的主要入口必须出现在需求、用例和报告流程中。

### 4.2 项目内部导航

```text
项目
├── 项目概览
├── 需求管理
├── 测试用例
├── 接口自动化
│   ├── 接口测试工作台
│   └── 接口自动化代码
├── UI 自动化
├── 测试任务
├── 测试报告
└── 项目设置
```

### 4.3 系统管理导航

```text
系统管理
├── 用户管理
├── AI 模型
├── Prompt 模板
├── Jenkins 配置
└── Git Credentials
```

---

## 5. 核心对象模型

V1 只保留支撑核心闭环的必要实体：

```text
User
Project
ProjectMember
Requirement
TestCase
AutomationProject
TestTask
TestExecution
TestReport
AIModel
PromptTemplate
```

### 5.1 关系

```text
Project
├── Requirement
├── TestCase
├── AutomationProject
├── TestTask
├── TestExecution
└── TestReport
```

基础追溯关系：

```text
Requirement → TestCase → AutomationProject → TestTask → TestExecution → TestReport
```

平台建立需求、测试用例、自动化项目、测试任务、执行记录和报告之间的基础关联关系。

---

## 6. 首页

首页展示平台和当前用户最需要关注的工作信息：

### 6.1 我的项目

- 项目名称；
- 项目状态；
- 最近执行；
- 最近通过率；
- 最近失败数量。

### 6.2 最近执行

- 项目；
- 测试类型；
- 测试任务；
- 执行时间；
- 状态；
- 通过率；
- 查看报告。

### 6.3 测试统计

- 项目数量；
- 测试用例数量；
- 接口自动化项目数量；
- UI 自动化项目数量；
- 最近执行次数；
- 最近通过率。

### 6.4 最近失败

- 测试任务；
- 失败用例；
- Jenkins Build；
- 失败时间；
- AI 分析状态。

### 6.5 Jenkins 状态

- Jenkins 连接状态；
- 最近构建成功；
- 最近构建失败；
- 当前构建中任务。

---

## 7. 项目管理

### 7.1 创建项目

项目字段：

- 项目名称；
- 项目标识；
- 项目描述；
- 项目负责人；
- 项目成员；
- 默认环境；
- 创建时间；
- 项目状态。

### 7.2 项目概览

展示：

- 项目基本信息；
- 需求数量；
- 测试用例数量；
- 接口自动化项目数量；
- UI 自动化项目数量；
- 测试任务数量；
- 最近执行；
- 最近报告；
- 最近 AI 失败分析。

### 7.3 业务项目与自动化项目

业务项目是测试资产的容器。一个业务项目下可以关联多个自动化项目。

例如：

```text
电商系统
├── 接口自动化：用户 API 自动化
├── 接口自动化：订单 API 自动化
├── UI 自动化：Web UI 自动化
└── UI 自动化：Admin UI 自动化
```

---

## 8. 需求管理

### 8.1 需求文档

- 上传 PDF；
- 上传 DOCX；
- 上传 Markdown；
- 上传 TXT；
- 查看需求文档；
- 保存文档名称、格式、上传人和上传时间；
- 记录简单版本号；
- 将需求文档用于 AI 测试用例生成。

### 8.2 需求与用例关联

每个需求文档可以关联 AI 生成的测试用例。测试用例保存后保留来源需求标识，便于后续追溯。

---

## 9. AI 测试用例生成

### 9.1 业务流程

```text
上传 PRD
  ↓
选择 AI 模型
  ↓
选择 Prompt 模板
  ↓
AI 解析需求
  ↓
生成结构化测试用例 Draft
  ↓
人工 Review
  ↓
保存为正式测试用例
```

### 9.2 支持模型

平台支持通过统一接口切换：

- GPT；
- DeepSeek；
- GLM。

业务模块只调用简单的统一能力：

```text
generate(prompt, model)
```

模型配置通过统一 Provider 接口调用，业务用户可以根据任务选择可用模型。

### 9.3 生成字段

- 用例编号；
- 用例标题；
- 所属需求；
- 所属模块；
- 前置条件；
- 测试步骤；
- 测试数据；
- 预期结果；
- 优先级；
- 测试类型；
- 标签。

### 9.4 Review 规则

- AI 生成结果先进入 Draft；
- 测试开发可以编辑、删除或合并 Draft；
- Review 通过后保存为正式测试用例；
- 采用轻量人工 Review；
- AI 输出需要标记模型、Prompt 模板和生成时间。

---

## 10. 测试用例管理

### 10.1 基础能力

- 手工创建；
- 编辑；
- 删除；
- 查询；
- 按模块筛选；
- 按优先级筛选；
- 按标签筛选；
- 查看用例状态；
- 查看简单版本记录；
- 导出。

### 10.2 用例状态

```text
DRAFT
REVIEWING
ACTIVE
ARCHIVED
```

### 10.3 用例来源

- 手工创建；
- AI 生成后 Review；
- 从需求文档关联。

测试用例可以关联来源需求和自动化项目，Review 采用轻量处理。

---

## 11. 测试任务

V1 使用一个简单的“测试任务”组织一次测试执行，不再拆分测试计划、测试套件和测试任务层级。

### 11.1 测试任务用途

例如：

```text
电商系统 V1.8 回归测试
支付接口 Smoke Test
Web UI 回归测试
```

Smoke Test、Regression Test 等作为任务类型或标签，不建立独立测试套件实体。

### 11.2 测试任务字段

- 测试任务名称；
- 测试类型；
- 标签；
- 自动化项目；
- Environment；
- Jenkins Job；
- Branch；
- Execute Parameters；
- 执行命令；
- 创建人；
- 创建时间；
- 当前状态；
- 最近执行记录。

### 11.3 执行流程

```text
创建测试任务
  ↓
选择自动化项目
  ↓
选择环境、Branch 和参数
  ↓
点击立即执行
  ↓
Jenkins Build
  ↓
测试执行
  ↓
Allure
  ↓
平台保存执行记录并展示结果
```

### 11.4 测试任务状态

```text
READY
RUNNING
PASSED
FAILED
CANCELLED
```

---

## 12. 接口自动化项目

### 12.1 产品定位

接口自动化 V1 是已有接口自动化测试代码的项目管理和执行入口。

代码由测试开发工程师在本地 IDE 中开发、调试和维护，推荐技术栈为：

- pytest；
- requests；
- 企业现有接口测试框架。

平台不生成、不编辑、不保存完整接口自动化代码作为事实来源。

### 12.2 项目字段

- 自动化项目名称；
- 项目类型：接口自动化；
- Git Repository URL；
- Branch；
- Git Credentials；
- Framework；
- Test Path；
- Execute Command；
- Environment；
- Jenkins Job；
- 描述；
- 负责人；
- 创建时间。

### 12.3 一个业务项目关联多个接口项目

```text
电商系统
├── 用户 API 自动化
├── 商品 API 自动化
├── 订单 API 自动化
└── 支付 API 自动化
```

### 12.4 管理能力

- 创建、编辑、删除自动化项目；
- 查看 Repository 信息；
- 查看当前 Branch；
- 配置测试路径和执行命令；
- 选择执行环境；
- 关联 Jenkins Job；
- 查看最近执行；
- 跳转 Git Repository；
- 跳转 Jenkins Build。

### 12.5 接口测试工作台

接口测试工作台是项目内用于接口调试、接口集合管理和快速验证的轻量工具入口。它与“接口自动化代码”是两个不同的能力：

- 接口测试工作台面向单个接口和接口集合的快速验证；
- 接口自动化代码面向测试开发工程师维护的正式自动化测试代码；
- 工作台中的请求和集合可以作为测试资产沉淀，但不替代 Git Repository 中的正式自动化代码；
- 平台不通过工作台生成完整的 pytest + requests 工程，也不提供在线 IDE。

工作台支持：

- 创建、编辑和删除接口集合；
- 在集合中管理多个接口请求；
- 选择请求方法和请求地址；
- 管理 Query、Path、Header 和 Body 等请求参数；
- 支持 JSON、Form Data 等常用请求体；
- 配置项目环境并使用环境变量替换请求地址、Header 和参数值；
- 对敏感环境变量进行脱敏展示，敏感值不进入普通日志；
- 配置请求前置脚本和后置脚本，用于获取 Token、提取响应字段和准备测试数据；
- 配置响应断言，包括状态码、JSON Path、字段值和基础结构校验；
- 单独发送当前接口并查看响应状态、响应体、耗时和断言结果；
- 按集合执行多个接口，并配置执行顺序和失败处理策略；
- 查看工作台接口和接口集合的执行历史。

工作台执行结果可以关联项目测试任务和测试报告，但正式的接口自动化回归仍以 Git Repository 中的代码通过 Jenkins 执行为准。

---

## 13. UI 自动化项目

### 13.1 产品定位

UI 自动化与接口自动化采用一致的企业工作模式：

```text
本地 IDE 开发
  ↓
Git Repository
  ↓
Jenkins
  ↓
Playwright / Selenium
  ↓
Allure
  ↓
平台查看结果
```

平台优先支持 Playwright，并保留 Selenium 的扩展能力。自动化代码由测试开发工程师在本地 IDE 开发和维护，平台负责项目配置、执行编排和结果管理。

原因是 UI 自动化高度依赖真实页面 DOM、Locator、页面状态、等待策略、浏览器环境和具体业务实现，仅凭 PRD 无法稳定生成可执行脚本。

### 13.2 项目字段

- 自动化项目名称；
- 项目类型：UI 自动化；
- Git Repository URL；
- Branch；
- Git Credentials；
- Framework；
- Test Path；
- Execute Command；
- Environment；
- Jenkins Job；
- 描述；
- 负责人；
- 创建时间。

### 13.3 管理能力

- 创建、编辑、删除 UI 自动化项目；
- 配置 Repository 和 Branch；
- 配置 Playwright 或 Selenium；
- 配置测试路径和执行命令；
- 配置 Jenkins Job；
- 选择执行环境；
- 查看执行历史；
- 查看日志、截图、视频和 Allure 报告。

---

## 14. Git 集成

### 14.1 Source of Truth

Git Repository 是自动化代码的唯一事实来源。平台只保存仓库引用和执行配置，不把完整代码复制到数据库中作为正式资产。

### 14.2 支持信息

- Repository URL；
- Branch；
- Credentials；
- Framework；
- Test Path；
- Execute Command；
- Jenkins Job。

V1 优先支持一种 Git 服务，具体由实施阶段确定为 GitLab 或 GitHub；兼容更多 Git 服务不作为 V1 的架构前提。

### 14.3 代码职责

- 测试开发工程师在本地 IDE 中开发和维护代码；
- 测试开发工程师提交 Commit 和 Push；
- Jenkins 根据平台配置 Checkout 指定 Branch；
- 平台不提供在线代码编辑、代码 Review 或代码合并能力。

---

## 15. 测试执行中心

### 15.1 执行方式

统一执行链路：

```text
平台
  ↓ Jenkins API
Jenkins
  ↓
Jenkins Agent / Server
  ↓
Git Checkout
  ↓
pytest / Playwright
  ↓
Allure Results
  ↓
Allure Report
  ↓
平台展示
```

Jenkins Agent 或 Server 负责实际测试执行，平台负责任务管理和结果展示。

### 15.2 平台职责

- 创建测试任务；
- 校验执行配置；
- 调用 Jenkins API；
- 触发 Build；
- 轮询 Build 状态；
- 获取 Build Number、Branch、Commit 和时间；
- 获取 Console Log；
- 获取测试结果；
- 关联 Allure Report；
- 保存执行历史；
- 展示截图、视频和错误信息。

### 15.3 执行记录字段

- 项目；
- 自动化项目；
- 测试任务；
- 测试类型；
- Branch；
- Commit；
- Environment；
- Jenkins Job；
- Build Number；
- 开始时间；
- 结束时间；
- 执行耗时；
- 状态；
- 日志地址；
- 报告地址。

### 15.4 执行状态

```text
PENDING
RUNNING
PASSED
FAILED
CANCELLED
```

---

## 16. CI/CD 与 Jenkins

### 16.1 管理员配置

- Jenkins URL；
- Jenkins Credentials；
- 连接测试；
- 可用 Job 查询。

### 16.2 项目执行配置

- Jenkins Job；
- Branch；
- Execute Parameters；
- Environment；
- Test Path；
- Execute Command。

### 16.3 手动执行

```text
测试开发点击立即执行
  ↓
平台调用 Jenkins API
  ↓
Jenkins Build
  ↓
Jenkins Agent / Server Checkout Git
  ↓
执行 pytest / Playwright
  ↓
生成 Allure Results
  ↓
平台获取状态和报告
```

### 16.4 CI 自动执行

Git Push 后由 Jenkins 按既有 Pipeline 或 Job 触发测试。平台负责展示关联的 Build 和结果，不自研 CI/CD。

### 16.5 平台展示

- Build Number；
- Branch；
- Commit；
- Start Time；
- End Time；
- Status；
- Console Log；
- Test Result；
- Allure Report；
- Screenshot；
- Video。

---

## 17. Allure 测试报告

平台不重新实现测试报告系统，使用 Jenkins 产出的 Allure Results 或 Allure Report。

### 17.1 报告内容

- Total；
- Passed；
- Failed；
- Broken；
- Skipped；
- Duration；
- Test Suites；
- Test Case；
- Error；
- Logs；
- Screenshot；
- Video。

### 17.2 报告能力

- 查看当前执行报告；
- 跳转 Allure Report；
- 查看测试用例详情；
- 查看失败错误；
- 查看日志、截图和视频；
- 保存历史执行记录；
- 查看历史报告。

报告中心聚焦当前执行结果、历史执行记录和 Allure 报告查看。

---

## 18. AI 失败分析

### 18.1 业务流程

```text
Jenkins 测试失败
  ↓
平台读取错误信息、日志和测试结果
  ↓
调用 AI
  ↓
失败原因分析
  ↓
错误分类
  ↓
可能原因和修复建议
```

### 18.2 失败分类

V1 可以使用简单分类：

- 环境问题；
- 测试数据问题；
- 接口或页面变更；
- 测试代码问题；
- 网络问题；
- 未知问题。

### 18.3 分析结果

AI 输出：

- 可能原因；
- 相关错误；
- 建议检查位置；
- 建议修复方向；
- 风险提示；
- 使用的模型和 Prompt 模板。

页面必须明确显示：

> AI 分析结果，仅供测试开发人员参考，不代表最终缺陷结论。

### 18.4 分析结果处理

测试开发根据 AI 建议在本地 IDE 修复代码，提交 Git 后通过 Jenkins 重跑。

---

## 19. 环境配置

环境配置用于为测试任务和自动化项目提供执行参数。

### 19.1 环境示例

- DEV；
- TEST；
- STAGING。

### 19.2 配置内容

- Environment 名称；
- Base URL；
- 必要环境变量；
- 测试配置；
- Jenkins 参数映射。

### 19.3 约束

- 环境配置只服务于测试任务和自动化项目；
- Secret 使用 Jenkins Credentials、环境变量或数据库加密存储；
- 敏感信息不得出现在前端、Git、命令行参数和普通日志中；
- 支持通过 Jenkins 参数传递必要的测试配置。

---

## 20. AI 能力与 Prompt 模板

### 20.1 AI 业务入口

AI 主要出现在：

- 需求管理：生成测试用例；
- 测试用例：Review AI Draft；
- 测试报告：分析失败结果。

AI 能力不是独立聊天窗口，也不是独立配置中心。

### 20.2 AI 模型配置

AIModel 字段：

- Provider；
- API Endpoint；
- API Key；
- Model Name；
- Temperature；
- Max Tokens；
- 启用状态。

管理员维护可用模型，业务用户在需求分析或失败分析页面选择模型。

### 20.3 Prompt Template

平台提供 Prompt 模板：

- 测试用例生成；
- 边界值测试；
- 异常测试；
- 测试结果分析。

模板字段：

- 模板名称；
- 任务类型；
- 模板内容；
- 版本；
- 启用状态；
- 创建人；
- 更新时间。

测试规则作为 Prompt 模板的配置项使用。

### 20.4 AI 调用记录

平台记录必要的 AI 调用审计信息：

- 用户；
- 项目；
- 模型；
- 任务类型；
- Prompt 模板；
- 请求时间；
- 调用状态。

调用记录用于审计、问题排查和结果追溯。

---

## 21. 项目设置

项目设置包括：

- 项目基本信息；
- 项目成员；
- 默认环境；
- 自动化项目；
- Jenkins Job 映射；
- Git Credentials 引用；
- AI 默认模型；
- AI 默认 Prompt 模板。

项目设置不包括缺陷系统映射、复杂权限矩阵、复杂测试数据和性能测试配置。

---

## 22. 核心状态流转

### 22.1 需求与用例

```text
上传需求
  ↓
AI 解析
  ↓
测试用例 Draft
  ↓
人工 Review
  ↓
正式测试用例
```

### 22.2 自动化项目

```text
创建
  ↓
配置 Git / Jenkins / Environment
  ↓
Ready
  ↓
触发测试任务
  ↓
执行中
  ↓
Passed / Failed
```

### 22.3 测试任务与报告

```text
Test Task
  ↓
Jenkins Build
  ↓
Test Execution
  ↓
Allure Report
  ↓
AI Failure Analysis
```

### 22.4 失败修复闭环

```text
测试失败
  ↓
查看日志、截图、视频和 Allure
  ↓
AI 分析失败原因
  ↓
测试开发本地修复代码
  ↓
Git Push
  ↓
Jenkins 再次执行
```

---

## 23. 数据与安全要求

### 23.1 数据原则

- 平台数据库保存项目、需求、用例、执行配置、执行记录和报告引用；
- Git 保存自动化代码；
- Jenkins 保存构建过程和执行环境；
- Allure 保存测试报告基础结果；
- 平台不复制完整自动化代码作为事实来源。

### 23.2 安全要求

- 密码不得明文保存；
- AI API Key 不返回前端；
- Git Credentials 不明文展示；
- Jenkins Credentials 不进入普通日志；
- 敏感配置不得提交 Git；
- 失败分析发送给 AI 的内容需要经过必要的敏感信息处理；
- 执行参数不得直接拼接未经校验的敏感值。

### 23.3 实现方式

优先使用：

- Jenkins Credentials；
- 环境变量；
- 数据库加密存储。

Secret 优先使用 Jenkins Credentials、环境变量或数据库加密存储。

---

## 24. 技术架构建议

### 24.1 Frontend

- Vue 3；
- TypeScript；
- Element Plus。

### 24.2 Backend

- Spring Boot；
- MyBatis 或 MyBatis-Plus；
- Spring Security。

### 24.3 Database

- MySQL。

### 24.4 AI

- GPT；
- DeepSeek；
- GLM；
- 统一简单 AI Provider 接口。

### 24.5 Automation

- pytest；
- requests；
- Playwright；
- Selenium 可作为扩展。

### 24.6 CI/CD 与报告

- Git；
- Jenkins；
- Allure。

### 24.7 Deployment

- Docker Compose；
- 单体 Spring Boot 应用优先。

系统采用单体应用和 Docker Compose 部署，执行能力由 Jenkins 提供。

---

## 25. 关键验收标准

### 25.1 基础能力

- [ ] 可以登录；
- [ ] 可以管理用户；
- [ ] 只有管理员和测试开发两种角色；
- [ ] 可以创建项目；
- [ ] 测试开发只能访问参与的项目。

### 25.2 需求与测试用例

- [ ] 可以上传 PDF、DOCX、Markdown、TXT；
- [ ] 可以查看需求文档；
- [ ] 可以选择 AI 模型和 Prompt 模板；
- [ ] 可以生成结构化测试用例 Draft；
- [ ] 可以人工 Review、编辑和保存；
- [ ] 可以手工创建、编辑、删除、查询测试用例。

### 25.3 自动化项目

- [ ] 可以创建接口自动化项目；
- [ ] 可以创建 UI 自动化项目；
- [ ] 接口自动化下可以进入接口测试工作台；
- [ ] 接口自动化下可以进入接口自动化代码项目管理；
- [ ] 工作台可以创建接口集合并管理多个接口请求；
- [ ] 工作台可以配置环境变量、请求参数、断言和前后置脚本；
- [ ] 工作台可以发送单个接口并查看响应结果；
- [ ] 工作台可以执行接口集合并查看执行历史；
- [ ] 可以配置 Git Repository URL；
- [ ] 可以配置 Branch、Credentials、Test Path 和 Execute Command；
- [ ] 可以配置 Environment；
- [ ] 可以配置 Jenkins Job；
- [ ] 一个业务项目可以关联多个自动化项目。

### 25.4 测试执行

- [ ] 平台可以调用 Jenkins API；
- [ ] Jenkins 可以 Checkout 指定 Branch；
- [ ] Jenkins 可以执行 pytest 或 Playwright；
- [ ] 平台可以获取 Build Number、Commit、状态和时间；
- [ ] 平台可以查看 Console Log；
- [ ] Jenkins Agent 或 Server 可以执行用户自动化代码。

### 25.5 测试报告

- [ ] 可以查看 Total、Passed、Failed、Broken、Skipped 和 Duration；
- [ ] 可以查看 Test Case、Error 和 Logs；
- [ ] 可以查看 Screenshot 和 Video；
- [ ] 可以查看 Allure Report；
- [ ] 可以保存历史执行记录；
- [ ] 可以从测试任务进入执行详情和报告。

### 25.6 AI 失败分析

- [ ] 可以对失败执行发起 AI 分析；
- [ ] 可以展示失败分类、可能原因和修复建议；
- [ ] 页面明确标记“AI 分析结果，仅供参考”；
- [ ] 测试开发可以根据分析结果修复代码并提交 Git；
- [ ] 测试开发修复后可以再次触发 Jenkins。

---

## 26. MVP Scope

产品首个可交付版本以一条真实可运行的浏览器演示闭环作为完成标准。

### 26.1 基础

- 登录；
- 用户；
- 管理员和测试开发两种角色；
- 项目创建和项目成员。

### 26.2 功能测试

- PRD 上传；
- PRD 解析；
- AI 模型选择；
- Prompt 模板选择；
- AI 生成测试用例；
- 测试用例 Review；
- 测试用例正式保存、查询和维护。

### 26.3 自动化项目

- 创建接口自动化项目；
- 创建 UI 自动化项目；
- 关联 Git Repository；
- 配置 Branch；
- 配置 Jenkins Job；
- 配置 Test Path；
- 配置 Execute Command；
- 配置测试环境。

### 26.4 执行与报告

- 平台触发 Jenkins；
- Jenkins 执行 pytest 或 Playwright；
- 获取 Build 状态和 Console Log；
- 保存执行历史；
- 查看 Allure 报告；
- 查看测试结果、Screenshot、Video 和错误信息。

### 26.5 AI

- PRD → 测试用例；
- 测试失败 → AI 分析；
- 支持 GPT、DeepSeek、GLM 中的模型切换；
- AI 分析结果可追溯到对应测试执行。

核心演示流程：

```text
PRD
→ AI 测试用例
→ Git 自动化项目
→ Jenkins
→ pytest / Playwright
→ Allure
→ AI 失败分析
```
