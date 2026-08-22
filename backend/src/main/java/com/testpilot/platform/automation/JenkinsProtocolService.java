package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JenkinsProtocolService {

    private static final String TEMPLATE_VERSION = "v2";

    private final AutomationProjectRepository projectRepository;
    private final JenkinsPipelineBindingRepository bindingRepository;
    private final ProjectAccessService projectAccessService;

    public JenkinsProtocolService(
            AutomationProjectRepository projectRepository,
            JenkinsPipelineBindingRepository bindingRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectRepository = projectRepository;
        this.bindingRepository = bindingRepository;
        this.projectAccessService = projectAccessService;
    }

    public JenkinsProtocolDtos.ContractResponse contract(
            Long projectId, Long automationProjectId, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        AutomationProject project = projectRepository.findByIdAndProjectId(automationProjectId, projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        return new JenkinsProtocolDtos.ContractResponse(
                TEMPLATE_VERSION,
                project.getAutomationType(),
                pipelineTemplate(project),
                Map.of(
                        "TEST_ENV", "任务环境",
                        "TEST_PATH", "测试路径",
                        "TEST_COMMAND", "执行命令",
                        "TEST_SUITE", "测试套件",
                        "REPORT_DIR", "allure-results",
                        "PLATFORM_EXECUTION_ID", "平台执行记录 ID",
                        "PLATFORM_CALLBACK_URL", "平台回调地址"
                ),
                "平台只提供预览，不覆盖仓库已有 Jenkinsfile。无 Jenkinsfile 时需由项目管理员确认后提交。"
        );
    }

    public JenkinsProtocolDtos.PrecheckResponse precheck(
            Long projectId, Long automationProjectId, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        projectRepository.findByIdAndProjectId(automationProjectId, projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        JenkinsPipelineBinding binding = bindingRepository
                .findFirstByAutomationProjectIdOrderByCreatedAtAsc(automationProjectId)
                .orElseThrow(() -> new BusinessException(40063, "尚未绑定 Jenkins Pipeline"));
        return new JenkinsProtocolDtos.PrecheckResponse(
                "QUEUED",
                "AGENT_PIPELINE",
                binding.getId(),
                Map.of(
                        "runtime", "PENDING",
                        "browser", "PENDING",
                        "driver", "PENDING",
                        "targetEnvironment", "PENDING",
                        "reportTool", "PENDING"
                ),
                "预检必须在正式任务使用的 Jenkins Agent/容器中执行。当前已生成预检协议，等待绑定流水线执行。"
        );
    }

    public String pipelineTemplate(AutomationProject project) {
        String command = project.getExecuteCommand() == null || project.getExecuteCommand().isBlank()
                ? "pytest ${TEST_PATH ?: ''}" : project.getExecuteCommand();
        return """
                pipeline {
                  agent any
                  parameters {
                    string(name: 'TEST_ENV', defaultValue: 'TEST')
                    string(name: 'TEST_PATH', defaultValue: '%s')
                    string(name: 'TEST_COMMAND', defaultValue: '%s')
                    string(name: 'TEST_SUITE', defaultValue: '')
                    string(name: 'REPORT_DIR', defaultValue: 'allure-results')
                    string(name: 'PLATFORM_EXECUTION_ID', defaultValue: '')
                    string(name: 'PLATFORM_CALLBACK_URL', defaultValue: '')
                  }
                  stages {
                    stage('Agent precheck') { steps { sh 'node --version || true; python --version || true' } }
                    stage('Test') { steps { sh "${TEST_COMMAND}" } }
                    stage('Offline Allure') {
                      steps {
                        sh '''
                          if command -v allure >/dev/null 2>&1 && [ -d "$REPORT_DIR" ]; then
                            allure generate "$REPORT_DIR" --clean -o allure-report
                          fi
                          if command -v zip >/dev/null 2>&1 && [ -d allure-report ]; then
                            zip -qr allure-report.zip allure-report
                          fi
                        '''
                      }
                    }
                  }
                  post {
                    always {
                      archiveArtifacts artifacts: 'allure-summary.json,allure-report/widgets/summary.json,test-summary.json,console.log,screenshots/**,videos/**,allure-report.zip', allowEmptyArchive: true, fingerprint: true
                    }
                  }
                }
                """.formatted(
                project.getTestPath() == null ? "" : project.getTestPath(),
                command.replace("'", "\\'")
        );
    }

    public String pipelineConfigXml(AutomationProject project, String script) {
        return """
                <flow-definition plugin="workflow-job">
                  <description>{DESCRIPTION}</description>
                  <keepDependencies>false</keepDependencies>
                  <properties/>
                  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps">
                    <script>{SCRIPT}</script>
                    <sandbox>true</sandbox>
                  </definition>
                  <triggers/>
                  <disabled>false</disabled>
                </flow-definition>
                """
                .replace("{DESCRIPTION}", xmlEscape(project.getName()))
                .replace("{SCRIPT}", xmlEscape(script));
    }

    public String freestyleConfigXml(AutomationProject project) {
        String command = project.getExecuteCommand() == null || project.getExecuteCommand().isBlank()
                ? "pytest" : project.getExecuteCommand();
        String credentialsId = project.getCredentialRef() == null || project.getCredentialRef().isBlank()
                ? "" : "\n                  <credentialsId>" + xmlEscape(project.getCredentialRef()) + "</credentialsId>";
        return """
                <project>
                  <description>{DESCRIPTION}</description>
                  <keepDependencies>false</keepDependencies>
                  <properties/>
                  <scm class="hudson.plugins.git.GitSCM" plugin="git">
                    <configVersion>2</configVersion>
                    <userRemoteConfigs>
                      <hudson.plugins.git.UserRemoteConfig>
                        <url>{REPOSITORY_URL}</url>{CREDENTIALS}
                      </hudson.plugins.git.UserRemoteConfig>
                    </userRemoteConfigs>
                    <branches>
                      <hudson.plugins.git.BranchSpec>
                        <name>{BRANCH}</name>
                      </hudson.plugins.git.BranchSpec>
                    </branches>
                    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
                    <submoduleCfg class="empty-list"/>
                    <extensions/>
                  </scm>
                  <canRoam>true</canRoam>
                  <disabled>false</disabled>
                  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
                  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
                  <triggers/>
                  <concurrentBuild>false</concurrentBuild>
                  <builders>
                    <hudson.tasks.Shell>
                      <command>{COMMAND}</command>
                      <configuredLocalRules/>
                    </hudson.tasks.Shell>
                  </builders>
                  <publishers>
                    <hudson.tasks.ArtifactArchiver>
                      <artifacts>allure-results/**,allure-report/**,allure-report.zip,test-summary.json</artifacts>
                      <allowEmptyArchive>true</allowEmptyArchive>
                      <onlyIfSuccessful>false</onlyIfSuccessful>
                      <fingerprint>false</fingerprint>
                      <defaultExcludes>true</defaultExcludes>
                      <caseSensitive>true</caseSensitive>
                    </hudson.tasks.ArtifactArchiver>
                  </publishers>
                  <buildWrappers/>
                </project>
                """
                .replace("{DESCRIPTION}", xmlEscape(project.getName()))
                .replace("{REPOSITORY_URL}", xmlEscape(project.getRepositoryUrl()))
                .replace("{CREDENTIALS}", credentialsId)
                .replace("{BRANCH}", xmlEscape(project.getBranch()))
                .replace("{COMMAND}", xmlEscape(command));
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
