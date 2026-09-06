package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import java.time.LocalDateTime;
import java.util.List;

public interface JenkinsClient {

    TriggerResult trigger(AutomationProject automationProject, TestTask task);

    SyncResult sync(AutomationProject automationProject, TestTask task, TestExecution execution);

    default TriggerResult trigger(
            AutomationProject automationProject,
            TestTask task,
            JenkinsTarget target
    ) {
        return trigger(automationProject, task);
    }

    default TriggerResult trigger(
            AutomationProject automationProject,
            TestTask task,
            JenkinsTarget target,
            String parametersJson
    ) {
        return trigger(automationProject, task, target);
    }

    default SyncResult sync(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        return sync(automationProject, task, execution);
    }

    default void testConnection(JenkinsTarget target) {
        throw new BusinessException(50101, "当前 Jenkins 客户端不支持连接测试");
    }

    default List<JenkinsJob> listJobs(JenkinsTarget target) {
        throw new BusinessException(50102, "当前 Jenkins 客户端不支持 Job 发现");
    }

    default void cancel(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        throw new BusinessException(50103, "当前 Jenkins 客户端不支持取消 Build");
    }

    default byte[] downloadAllureArchive(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        throw new BusinessException(50105, "当前 Jenkins 客户端不支持下载 Allure 报告");
    }

    default AllureSummary downloadAllureSummary(
            AutomationProject automationProject,
            TestTask task,
            TestExecution execution,
            JenkinsTarget target
    ) {
        throw new BusinessException(50106, "当前 Jenkins 客户端不支持读取 Allure 统计");
    }

    default String fetchConsoleLog(JenkinsTarget target, String consoleLogRef) {
        throw new BusinessException(50107, "当前 Jenkins 客户端不支持读取控制台日志");
    }

    default void createJob(JenkinsTarget target, String jobName, String configXml) {
        throw new BusinessException(50104, "当前 Jenkins 客户端不支持创建 Job");
    }

    record JenkinsTarget(
            String baseUrl,
            String username,
            String apiToken,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
    }

    record TriggerResult(String buildNumber, String queueUrl) {
    }

    record AllureSummary(
            int totalCount,
            int passedCount,
            int failedCount,
            int brokenCount,
            int skippedCount
    ) {
    }

    record JenkinsJob(
            String name,
            String path,
            String url,
            String pipelineType,
            String jenkinsClass,
            boolean buildable,
            String parametersJson
    ) {
    }

    record SyncResult(
            String buildNumber,
            String queueUrl,
            String status,
            String commitSha,
            String consoleLogRef,
            String allureUrl,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int totalCount,
            int passedCount,
            int failedCount,
            int brokenCount,
            int skippedCount,
            String artifactRef
    ) {
    }
}
