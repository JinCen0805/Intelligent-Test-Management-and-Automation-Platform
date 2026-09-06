package com.testpilot.platform.config;

import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectMember;
import com.testpilot.platform.project.ProjectMemberRepository;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import com.testpilot.platform.ai.PromptTemplate;
import com.testpilot.platform.ai.PromptTemplateRepository;
import com.testpilot.platform.ai.BuiltInPrompts;
import com.testpilot.platform.ai.AiModel;
import com.testpilot.platform.ai.AiModelRepository;
import com.testpilot.platform.automation.AutomationProject;
import com.testpilot.platform.automation.AutomationProjectRepository;
import com.testpilot.platform.automation.TestTask;
import com.testpilot.platform.automation.TestTaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataConfig {

    @Bean
    @Profile({"local", "demo", "test"})
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            PromptTemplateRepository promptTemplateRepository,
            AutomationProjectRepository automationProjectRepository,
            TestTaskRepository testTaskRepository,
            ProjectMemberRepository projectMemberRepository,
            PasswordEncoder passwordEncoder,
            AiModelRepository aiModelRepository
    ) {
        return args -> {
            User developer = userRepository.findByUsername("lin.xiao")
                    .orElseGet(() -> userRepository.save(new User(
                            "lin.xiao",
                            passwordEncoder.encode("demo123"),
                            "林晓",
                            "lin.xiao@company.cn",
                            "TEST_DEVELOPER",
                            "ENABLED"
                    )));

            if (userRepository.findByUsername("chen.yu").isEmpty()) {
                userRepository.save(new User(
                        "chen.yu",
                        passwordEncoder.encode("demo123"),
                        "陈宇",
                        "chen.yu@company.cn",
                        "ADMIN",
                        "ENABLED"
                ));
            }

            if (projectRepository.countByDeletedFalse() == 0) {
                Project firstProject = projectRepository.save(new Project(
                        "星河电商平台",
                        "XH-MALL",
                        "电商核心交易链路，覆盖用户、商品、订单、支付与售后。",
                        developer.getId(),
                        "RUNNING"
                ));
                Project secondProject = projectRepository.save(new Project(
                        "供应链中台",
                        "SC-CORE",
                        "采购、库存和供应商协同测试项目。",
                        developer.getId(),
                        "RUNNING"
                ));
                projectMemberRepository.save(new ProjectMember(firstProject.getId(), developer.getId(), "PROJECT_OWNER"));
                projectMemberRepository.save(new ProjectMember(secondProject.getId(), developer.getId(), "PROJECT_OWNER"));
            } else {
                projectRepository.findAllByDeletedFalseOrderByCreatedAtDesc().forEach(project -> {
                    if (projectMemberRepository.findByProjectIdAndUserId(project.getId(), project.getOwnerId()).isEmpty()) {
                        projectMemberRepository.save(new ProjectMember(project.getId(), project.getOwnerId(), "PROJECT_OWNER"));
                    }
                });
            }

            if (promptTemplateRepository.findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc("TEST_CASE_GENERATION").isEmpty()) {
                promptTemplateRepository.save(new PromptTemplate(
                        "测试用例生成规范",
                        "TEST_CASE_GENERATION",
                        BuiltInPrompts.TEST_CASE_GENERATION,
                        "V3.1",
                        true
                ));
            }
            // Upgrade the original seeded prompt once while preserving any administrator-authored prompt.
            promptTemplateRepository.findByNameAndTaskType("测试用例生成规范", "TEST_CASE_GENERATION").stream()
                    .filter(prompt -> prompt.getContent() != null
                            && (prompt.getContent().startsWith("你是一名资深测试设计工程师。请根据用户上传的项目文件内容生成可直接执行的测试用例。")
                            || prompt.getContent().startsWith("根据需求正文生成测试用例，覆盖正向、异常、边界场景。")
                            || prompt.getContent().contains("如果系统为避免重复需要自动改成 TC-AI-xxx")
                            ))
                    .findFirst()
                    .ifPresent(prompt -> {
                        prompt.update(
                                prompt.getName(),
                                prompt.getTaskType(),
                                BuiltInPrompts.TEST_CASE_GENERATION,
                                "V3.1",
                                prompt.isEnabled()
                        );
                        promptTemplateRepository.save(prompt);
                    });
            promptTemplateRepository.findByNameAndTaskType("测试用例对话规范", "TEST_CASE_CHAT").stream()
                    .filter(prompt -> prompt.getContent() != null
                            && (prompt.getContent().startsWith("你是一名资深测试设计工程师和测试用例对话助手。请基于项目文件和现有用例，帮助用户分析、补充或修改测试用例。")
                            || prompt.getContent().startsWith("你是一名资深测试用例助手。")
                            || prompt.getContent().contains("模块大写缩写_三位序号或留空")))
                    .findFirst()
                    .ifPresent(prompt -> {
                        prompt.update(
                                prompt.getName(),
                                prompt.getTaskType(),
                                BuiltInPrompts.TEST_CASE_CHAT,
                                "V3.1",
                                prompt.isEnabled()
                        );
                        promptTemplateRepository.save(prompt);
                    });
            if (promptTemplateRepository.findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc("TEST_CASE_CHAT").isEmpty()) {
                promptTemplateRepository.save(new PromptTemplate(
                        "测试用例对话规范",
                        "TEST_CASE_CHAT",
                        BuiltInPrompts.TEST_CASE_CHAT,
                        "V3.1",
                        true
                ));
            }
            if (promptTemplateRepository.findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc("FAILURE_ANALYSIS").isEmpty()) {
                promptTemplateRepository.save(new PromptTemplate(
                        "失败分析规范",
                        "FAILURE_ANALYSIS",
                        "你是一名资深测试开发工程师，请根据自动化测试失败信息进行根因分析。仅输出 JSON，不要 Markdown 代码块。JSON 结构必须包含：category（取值 ENVIRONMENT/TEST_DATA/API_CHANGE/TEST_CODE/NETWORK/UNKNOWN 之一）、possibleCauses、relatedErrors、checkLocations、fixDirections、riskWarnings，其中后五项为字符串数组。",
                        "V1.0",
                        true
                ));
            }
            if (promptTemplateRepository.findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc("AUTOMATION_TASK_GENERATION").isEmpty()) {
                promptTemplateRepository.save(new PromptTemplate(
                        "自动化任务生成规范",
                        "AUTOMATION_TASK_GENERATION",
                        "你是一名资深测试架构师，请根据 Git 仓库、Branch 和自然语言描述生成自动化测试任务配置。仅输出 JSON，不要 Markdown 代码块。JSON 字段：name（任务名称）、taskType（SMOKE/REGRESSION）、automationType（API/UI）、framework（PYTEST/PLAYWRIGHT/SELENIUM）、testPath、executeCommand、environment（TEST/STAGING/PROD）、parametersJson（JSON 字符串）、pipelineType（PIPELINE/FREESTYLE）、jenkinsJob。",
                        "V1.0",
                        true
                ));
            }

            projectRepository.findByCodeAndDeletedFalse("XH-MALL").ifPresent(project -> {
                if (automationProjectRepository.findAllByProjectIdOrderByCreatedAtDesc(project.getId()).isEmpty()) {
                    AutomationProject automationProject = automationProjectRepository.save(new AutomationProject(
                            project.getId(),
                            "订单 API 自动化",
                            "API",
                            "https://example.com/order-api.git",
                            "main",
                            null,
                            "pytest + requests",
                            "tests/api",
                            "pytest tests/api --alluredir=allure-results --env=test",
                            "TEST",
                            "xh-mall-api-pipeline"
                    ));
                    testTaskRepository.save(new TestTask(
                            project.getId(),
                            "订单接口回归",
                            "REGRESSION",
                            automationProject.getId(),
                            "TEST",
                            "main",
                            "xh-mall-api-pipeline",
                            "{\"alluredir\":\"allure-results\"}"
                    ));
                }
            });
        };
    }

    @Bean
    @Profile("test")
    CommandLineRunner seedTestAiModel(AiModelRepository aiModelRepository) {
        return args -> {
            if (aiModelRepository.findFirstByEnabledTrueOrderByCreatedAtAsc().isEmpty()) {
                aiModelRepository.save(new AiModel("测试 Mock 模型", "MOCK", "", null, true));
            }
        };
    }
}
