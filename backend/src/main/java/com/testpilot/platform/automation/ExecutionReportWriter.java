package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * Execution report writer extracted from AutomationService: terminal updates, report upsert, consoleLogRef validation.
 */
@Component
public class ExecutionReportWriter {

    /**
     * Only consoleText of a single build is allowed (prevents reading arbitrary Jenkins API paths).
     */
    private static final Pattern SAFE_CONSOLE_LOG_REF =
            Pattern.compile("^/job/[A-Za-z0-9_./-]+/\\d+/consoleText$");
    private static final List<String> TERMINAL_EXECUTION_STATUSES =
            List.of("PASSED", "FAILED", "ABORTED", "TIMEOUT");

    private final TestExecutionRepository testExecutionRepository;
    private final TestReportRepository testReportRepository;

    public ExecutionReportWriter(
            TestExecutionRepository testExecutionRepository,
            TestReportRepository testReportRepository
    ) {
        this.testExecutionRepository = testExecutionRepository;
        this.testReportRepository = testReportRepository;
    }

    public AutomationDtos.TestExecutionResponse toExecutionResponse(TestExecution execution) {
        return AutomationDtos.TestExecutionResponse.from(
                execution,
                testReportRepository.findByExecutionId(execution.getId()).orElse(null)
        );
    }

    public AutomationDtos.TestExecutionResponse saveReport(
            TestExecution execution,
            AutomationDtos.ExecutionResultRequest request,
            boolean trustedSource
    ) {
        validateReport(request);
        String incomingStatus = request.status().trim().toUpperCase();
        TestExecution current = testExecutionRepository.findById(execution.getId())
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (TERMINAL_EXECUTION_STATUSES.contains(current.getStatus())) {
            if (!incomingStatus.equals(current.getStatus())) {
                throw new BusinessException(40912, "执行已完成，禁止覆盖终态结果");
            }
            return toExecutionResponse(current);
        }

        String consoleLogRef = trustedSource
                ? trustedConsoleLogRef(execution, request.consoleLogRef())
                : resolveExternalConsoleLogRef(execution, request.consoleLogRef());
        LocalDateTime finishedAt = LocalDateTime.now();
        Long durationMs = execution.getStartedAt() == null
                ? null
                : Duration.between(execution.getStartedAt(), finishedAt).toMillis();
        int updated = testExecutionRepository.updateResultFromActive(
                execution.getId(),
                incomingStatus,
                execution.getBuildNumber(),
                execution.getQueueUrl(),
                blankToNull(request.commitSha()),
                consoleLogRef,
                execution.getStartedAt(),
                finishedAt,
                durationMs,
                LocalDateTime.now()
        );
        if (updated == 0) {
            TestExecution latest = testExecutionRepository.findById(execution.getId())
                    .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
            if (!TERMINAL_EXECUTION_STATUSES.contains(latest.getStatus())) {
                throw new BusinessException(40913, "执行状态已变更，请稍后重试");
            }
            if (!incomingStatus.equals(latest.getStatus())) {
                throw new BusinessException(40912, "执行已完成，禁止覆盖终态结果");
            }
            return toExecutionResponse(latest);
        }
        execution.applyResult(incomingStatus, blankToNull(request.commitSha()), consoleLogRef, finishedAt);
        TestReport savedReport = upsertReport(execution.getId(), request);
        return AutomationDtos.TestExecutionResponse.from(execution, savedReport);
    }

    /** 运行中进度用条件更新写入，已终态时自动放弃，避免覆盖回调写入的终态。 */
    public void persistActiveProgress(TestExecution execution) {
        testExecutionRepository.updateProgressIfActive(
                execution.getId(),
                execution.getBuildNumber(),
                execution.getQueueUrl(),
                execution.getCommitSha(),
                execution.getConsoleLogRef(),
                execution.getStartedAt(),
                LocalDateTime.now()
        );
    }

    /**
     * 报告 upsert：同一执行重复上报时复用原行（保留 AI 分析等字段），
     * 并发插入撞唯一键时回退到更新，避免 500。
     */
    private TestReport upsertReport(Long executionId, AutomationDtos.ExecutionResultRequest request) {
        TestReport report = testReportRepository.findByExecutionId(executionId).orElse(null);
        if (report == null) {
            try {
                return testReportRepository.save(new TestReport(
                        executionId,
                        blankToNull(request.allureUrl()),
                        request.totalCount(),
                        request.passedCount(),
                        request.failedCount(),
                        request.brokenCount(),
                        request.skippedCount(),
                        blankToNull(request.artifactRef()),
                        blankToNull(request.failureDetail()),
                        blankToNull(request.screenshotRef()),
                        blankToNull(request.videoRef()),
                        blankToNull(request.aiAnalysis())
                ));
            } catch (DuplicateKeyException exception) {
                report = testReportRepository.findByExecutionId(executionId)
                        .orElseThrow(() -> new BusinessException(40913, "报告写入冲突，请稍后重试"));
            }
        }
        report.updateFrom(
                blankToNull(request.allureUrl()),
                request.totalCount(),
                request.passedCount(),
                request.failedCount(),
                request.brokenCount(),
                request.skippedCount(),
                blankToNull(request.artifactRef()),
                blankToNull(request.failureDetail()),
                blankToNull(request.screenshotRef()),
                blankToNull(request.videoRef()),
                blankToNull(request.aiAnalysis())
        );
        return testReportRepository.save(report);
    }

    private String trustedConsoleLogRef(TestExecution execution, String incoming) {
        String provided = blankToNull(incoming);
        return provided != null ? provided : deriveConsoleLogRef(execution);
    }

    /**
     * 外部上报（手工上报 / Jenkins 回调）的 consoleLogRef 校验：只允许本次构建的 consoleText。
     * 允许两种形式：以 "/" 开头的 Jenkins 路径，或完整 http(s) URL（host 会被替换为配置的 Jenkins 地址）。
     */
    private String resolveExternalConsoleLogRef(TestExecution execution, String incoming) {
        String provided = blankToNull(incoming);
        if (provided == null) {
            return deriveConsoleLogRef(execution);
        }
        String path = extractJenkinsPath(provided);
        if (path == null || !SAFE_CONSOLE_LOG_REF.matcher(path).matches()) {
            throw new BusinessException(40043, "控制台日志引用不合法");
        }
        return path;
    }

    private String extractJenkinsPath(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return null;
            }
            return uri.getPath();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String deriveConsoleLogRef(TestExecution execution) {
        String job = execution.getJenkinsJob();
        String buildNumber = execution.getBuildNumber();
        if (job == null || job.isBlank() || buildNumber == null || buildNumber.isBlank()) {
            return null;
        }
        String jobPath = Arrays.stream(job.split("/"))
                .filter(segment -> !segment.isBlank())
                .map(segment -> "job/" + segment)
                .collect(Collectors.joining("/"));
        return "/" + jobPath + "/" + buildNumber + "/consoleText";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }


    private void validateReport(AutomationDtos.ExecutionResultRequest request) {
        String status = request.status().trim().toUpperCase();
        if (!List.of("RUNNING", "PASSED", "FAILED", "ABORTED", "TIMEOUT").contains(status)) {
            throw new BusinessException(40009, "执行状态不合法");
        }
        int sum = request.passedCount()
                + request.failedCount()
                + request.brokenCount()
                + request.skippedCount();
        if (request.totalCount() != sum) {
            throw new BusinessException(40010, "报告总数必须等于通过、失败、Broken 和跳过数量之和");
        }
    }
}
