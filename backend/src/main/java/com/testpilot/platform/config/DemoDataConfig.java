package com.testpilot.platform.config;

import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import com.testpilot.platform.ai.AiModel;
import com.testpilot.platform.ai.AiModelRepository;
import com.testpilot.platform.ai.PromptTemplate;
import com.testpilot.platform.ai.PromptTemplateRepository;
import com.testpilot.platform.automation.AutomationProject;
import com.testpilot.platform.automation.AutomationProjectRepository;
import com.testpilot.platform.automation.TestTask;
import com.testpilot.platform.automation.TestTaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataConfig {

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            AiModelRepository aiModelRepository,
            PromptTemplateRepository promptTemplateRepository,
            AutomationProjectRepository automationProjectRepository,
            TestTaskRepository testTaskRepository,
            PasswordEncoder passwordEncoder
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
                projectRepository.save(new Project(
                        "星河电商平台",
                        "XH-MALL",
                        "电商核心交易链路，覆盖用户、商品、订单、支付与售后。",
                        developer.getId(),
                        "TEST",
                        "RUNNING"
                ));
                projectRepository.save(new Project(
                        "供应链中台",
                        "SC-CORE",
                        "采购、库存和供应商协同测试项目。",
                        developer.getId(),
                        "STAGING",
                        "RUNNING"
                ));
            }

            if (aiModelRepository.count() == 0) {
                aiModelRepository.save(new AiModel(
                        "local-demo-model",
                        "MOCK",
                        "",
                        null,
                        true
                ));
            }
            if (promptTemplateRepository.count() == 0) {
                promptTemplateRepository.save(new PromptTemplate(
                        "测试用例生成规范",
                        "TEST_CASE_GENERATION",
                        "根据需求正文生成测试用例，覆盖正向、异常、边界场景。字段必须包含 title、module、precondition、steps、testData、expectedResult、priority、type、tags。",
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
}
