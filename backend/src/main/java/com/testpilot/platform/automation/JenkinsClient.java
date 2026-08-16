package com.testpilot.platform.automation;

import java.time.LocalDateTime;

public interface JenkinsClient {

    TriggerResult trigger(AutomationProject automationProject, TestTask task);

    SyncResult sync(AutomationProject automationProject, TestTask task, TestExecution execution);

    record TriggerResult(String buildNumber, String queueUrl) {
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
