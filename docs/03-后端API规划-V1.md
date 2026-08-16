# 后端 API 规划 V1

统一前缀：`/api`

统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 1. 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 登录 |
| POST | `/auth/logout` | 登出 |
| GET | `/auth/me` | 当前用户 |

## 2. 项目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects` | 项目列表 |
| POST | `/projects` | 创建项目 |
| GET | `/projects/{id}` | 项目详情 |
| PUT | `/projects/{id}` | 编辑项目 |
| DELETE | `/projects/{id}` | 删除项目 |
| GET | `/projects/{id}/members` | 成员列表 |
| POST | `/projects/{id}/members` | 添加成员 |

## 3. 需求和测试用例

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects/{id}/requirements` | 需求列表 |
| POST | `/projects/{id}/requirements/upload` | 上传需求 |
| POST | `/requirements/{id}/parse` | 解析需求 |
| GET | `/projects/{id}/test-cases` | 用例列表 |
| POST | `/projects/{id}/test-cases` | 创建用例 |
| PUT | `/test-cases/{id}` | 编辑用例 |
| DELETE | `/test-cases/{id}` | 删除用例 |
| POST | `/test-cases/{id}/review` | Review 用例 |

## 4. AI

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/ai/models` | 可用模型 |
| GET | `/ai/prompts` | Prompt 模板 |
| POST | `/ai/test-case-generation` | PRD 生成测试用例 |
| POST | `/ai/failure-analysis` | 分析测试失败 |
| GET | `/ai/call-records` | AI 调用记录 |

## 5. 接口测试工作台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects/{id}/api-collections` | 集合列表 |
| POST | `/projects/{id}/api-collections` | 创建集合 |
| PUT | `/api-collections/{id}` | 编辑集合 |
| DELETE | `/api-collections/{id}` | 删除集合 |
| GET | `/api-collections/{id}/requests` | 请求列表 |
| POST | `/api-collections/{id}/requests` | 创建请求 |
| PUT | `/api-requests/{id}` | 编辑请求 |
| DELETE | `/api-requests/{id}` | 删除请求 |
| GET | `/projects/{id}/api-environments/{environment}` | 环境变量 |
| PUT | `/projects/{id}/api-environments/{environment}` | 保存环境变量 |
| POST | `/api-requests/{id}/execute` | 执行单接口 |
| POST | `/api-collections/{id}/execute` | 执行接口集合 |
| GET | `/projects/{id}/api-executions` | 工作台执行历史 |

工作台执行接口请求，不执行 Git 仓库中的用户自动化代码。

## 6. 自动化项目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects/{id}/automation-projects` | 自动化项目列表 |
| POST | `/projects/{id}/automation-projects` | 创建自动化项目 |
| GET | `/automation-projects/{id}` | 自动化项目详情 |
| PUT | `/automation-projects/{id}` | 编辑自动化项目 |
| DELETE | `/automation-projects/{id}` | 删除自动化项目 |

## 7. 测试任务和执行

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects/{id}/test-tasks` | 测试任务列表 |
| POST | `/projects/{id}/test-tasks` | 创建测试任务 |
| PUT | `/test-tasks/{id}` | 编辑测试任务 |
| POST | `/test-tasks/{id}/execute` | 触发 Jenkins |
| GET | `/test-executions/{id}` | 执行详情 |
| GET | `/test-executions/{id}/log` | 获取执行日志 |
| GET | `/test-executions/{id}/report` | 获取报告 |

## 8. Jenkins

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/jenkins/config` | Jenkins 配置 |
| PUT | `/admin/jenkins/config` | 保存 Jenkins 配置 |
| GET | `/admin/jenkins/jobs` | Job 列表 |
| POST | `/jenkins/builds/{buildId}/callback` | Jenkins 结果回调 |

