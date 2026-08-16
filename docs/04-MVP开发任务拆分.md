# V1 MVP 开发任务拆分

## 目标

先跑通一条真实链路，再补齐外围管理能力：

```text
登录
→ 项目
→ 上传 PRD
→ AI 生成用例
→ Review
→ Git 自动化项目
→ Jenkins
→ pytest / Playwright
→ Allure
→ AI 失败分析
```

## 阶段 0：工程初始化

- [x] 创建前端工程；
- [x] 创建 Spring Boot 后端工程；
- [x] 实现健康检查、登录和项目基础 API；
- [ ] 创建 MySQL 数据库；
- [ ] 配置开发环境；
- [ ] 配置 Git 分支和提交规范；
- [ ] 建立接口文档和数据库迁移机制。

完成标准：前端、后端可以独立启动，后端可以连接数据库。

## 阶段 1：基础平台

- [x] 登录和登出；
- [ ] 当前用户；
- [ ] 用户管理；
- [ ] 管理员 / 测试开发角色；
- [ ] 项目创建、编辑、删除；
- [ ] 项目成员；
- [ ] 项目级权限；
- [ ] 项目布局和基础导航。

完成标准：管理员创建项目，测试开发可以访问参与项目。

## 阶段 2：需求和测试用例

- [x] PDF、DOCX、Markdown、TXT 上传；
- [x] 需求解析状态；
- [ ] 需求列表和详情；
- [ ] 手工创建、编辑、删除用例；
- [ ] 用例查询和筛选；
- [ ] Draft、Review、正式状态；
- [ ] 用例与需求关联。

完成标准：用户可以上传 PRD 并维护测试用例。

## 阶段 3：AI 测试用例生成

- [ ] AI Model 配置；
- [ ] Prompt Template；
- [ ] 模型选择；
- [ ] PRD 文本提交模型；
- [ ] 结构化 JSON 输出；
- [ ] 输出字段校验；
- [ ] Draft 预览；
- [ ] 人工 Review 后保存；
- [ ] 调用失败和超时提示。

完成标准：一份 PRD 可以生成并保存结构化测试用例。

## 阶段 4：接口自动化代码项目

- [ ] 创建接口自动化项目；
- [ ] 配置 Git Repository；
- [ ] 配置 Branch；
- [ ] 配置 Credentials 引用；
- [ ] 配置 Test Path；
- [ ] 配置 Execute Command；
- [ ] 配置 Environment；
- [ ] 配置 Jenkins Job；
- [ ] 展示最近 Commit。

完成标准：平台可以保存一个真实 pytest + requests 仓库的执行配置。

## 阶段 5：Jenkins 执行和报告

- [ ] Jenkins URL 和 Credentials 配置；
- [ ] 测试任务创建；
- [ ] 平台触发 Jenkins Build；
- [ ] 获取 Build Number；
- [ ] 获取 Branch 和 Commit；
- [ ] 获取执行状态；
- [ ] 获取 Console Log；
- [ ] 保存 TestExecution；
- [ ] 关联 Allure Report；
- [ ] 展示测试统计、错误、截图和视频。

完成标准：平台点击“立即执行”后，可以看到真实 Jenkins 结果和 Allure 报告。

## 阶段 6：接口测试工作台

- [ ] 接口集合；
- [ ] 接口请求；
- [ ] Query、Path、Header、Body；
- [ ] 环境变量；
- [ ] 变量替换；
- [ ] 前置和后置脚本；
- [ ] 状态码和 JSON Path 断言；
- [ ] 单接口发送；
- [ ] 集合执行；
- [ ] 执行历史。

完成标准：工作台可以完成一次接口调试和集合执行。

## 阶段 7：UI 自动化项目

- [ ] 创建 UI 自动化项目；
- [ ] Playwright Git 配置；
- [ ] Branch、Test Path、Execute Command；
- [ ] Environment；
- [ ] Jenkins Job；
- [ ] 执行历史；
- [ ] 日志、Screenshot、Video、Allure。

完成标准：平台可以通过 Jenkins 执行一个真实 Playwright 项目。

## 阶段 8：AI 失败分析

- [ ] 读取失败日志；
- [ ] 读取测试结果；
- [ ] 选择分析模型；
- [ ] 失败分类；
- [ ] 可能原因；
- [ ] 修复建议；
- [ ] 明确 AI 结果仅供参考；
- [ ] 关联执行记录、Branch 和 Commit。

完成标准：一次失败执行可以生成可追溯的 AI 分析结果。

## 阶段 9：验收和部署

- [ ] 功能测试；
- [ ] 接口测试；
- [ ] 权限测试；
- [ ] 敏感信息测试；
- [ ] AI 异常测试；
- [ ] Jenkins 异常测试；
- [ ] 浏览器主流程演示；
- [ ] Docker Compose 部署；
- [ ] 编写部署说明；
- [ ] 编写演示数据初始化脚本。

完成标准：新环境可以部署，浏览器可以完整演示 V1 闭环。
