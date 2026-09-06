package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final TestExecutionRepository executionRepository;
    private final TestReportRepository reportRepository;
    private final TestTaskRepository taskRepository;
    private final AutomationProjectRepository automationProjectRepository;

    public ReportService(
            ProjectRepository projectRepository,
            ProjectAccessService projectAccessService,
            TestExecutionRepository executionRepository,
            TestReportRepository reportRepository,
            TestTaskRepository taskRepository,
            AutomationProjectRepository automationProjectRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
        this.executionRepository = executionRepository;
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.automationProjectRepository = automationProjectRepository;
    }

    public ReportDtos.CenterResponse center(
            Long projectId,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            User user
    ) {
        List<Project> accessibleProjects = projectRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(project -> projectId == null || project.getId().equals(projectId))
                .filter(project -> projectAccessService.canRead(project, user))
                .toList();
        if (projectId != null && accessibleProjects.isEmpty()) {
            throw new BusinessException(40301, "没有项目访问权限");
        }
        List<Long> projectIds = accessibleProjects.stream().map(Project::getId).toList();
        if (projectIds.isEmpty()) {
            return new ReportDtos.CenterResponse(List.of(), 0, size, 0, 0,
                    new ReportDtos.Summary(0, 0, 0, 0, 0, 0), List.of());
        }
        LocalDate actualTo = to == null ? LocalDate.now() : to;
        LocalDate actualFrom = from == null ? actualTo.minusDays(29) : from;
        if (actualFrom.isAfter(actualTo)) {
            throw new BusinessException(40041, "报告开始日期不能晚于结束日期");
        }
        LocalDateTime fromAt = actualFrom.atStartOfDay();
        LocalDateTime toAt = actualTo.atTime(LocalTime.MAX);
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (normalizedStatus != null
                && !List.of("QUEUED", "RUNNING", "PASSED", "FAILED", "ABORTED", "TIMEOUT")
                .contains(normalizedStatus)) {
            throw new BusinessException(40009, "报告状态筛选条件不合法");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(40042, "报告分页参数不合法");
        }
        Page<TestExecution> pageResult = normalizedStatus == null
                ? executionRepository.findAllByProjectIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                        projectIds, fromAt, toAt, PageRequest.of(page, size))
                : executionRepository.findAllByProjectIdInAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        projectIds, normalizedStatus, fromAt, toAt,
                        PageRequest.of(page, size));
        TestExecutionMapper.ReportAggregate aggregate = executionRepository.aggregateReports(
                projectIds, normalizedStatus, fromAt, toAt);
        List<TestExecutionMapper.ReportTrend> trendRows = executionRepository.trendReports(
                projectIds, normalizedStatus, fromAt, toAt);
        return new ReportDtos.CenterResponse(
                toViews(pageResult.getContent(), accessibleProjects),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                summary(aggregate),
                trend(trendRows)
        );
    }

    public ReportDtos.ExecutionDetail detail(Long executionId, User user) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        Project project = projectAccessService.requirePermission(execution.getProjectId(), user, RbacPermissions.REPORT_READ);
        TestTask task = taskRepository.findById(execution.getTestTaskId()).orElse(null);
        AutomationProject automationProject = automationProjectRepository
                .findById(execution.getAutomationProjectId()).orElse(null);
        TestReport report = reportRepository.findByExecutionId(executionId).orElse(null);
        return ReportDtos.ExecutionDetail.from(execution, project, task, automationProject, report);
    }

    private List<ReportDtos.ExecutionView> toViews(List<TestExecution> executions, List<Project> projects) {
        Map<Long, String> projectNames = projects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        Map<Long, String> taskNames = taskRepository.findAllById(
                        executions.stream().map(TestExecution::getTestTaskId).toList()
                ).stream()
                .collect(Collectors.toMap(TestTask::getId, TestTask::getName));
        Map<Long, TestReport> reports = reportRepository.findAllByExecutionIdIn(
                        executions.stream().map(TestExecution::getId).toList()
                ).stream().collect(Collectors.toMap(TestReport::getExecutionId, report -> report));
        return executions.stream()
                .map(execution -> ReportDtos.ExecutionView.from(
                        execution,
                        projectNames.getOrDefault(execution.getProjectId(), "未知项目"),
                        taskNames.getOrDefault(execution.getTestTaskId(), "未知任务"),
                        reports.get(execution.getId())
                ))
                .toList();
    }

    private ReportDtos.Summary summary(TestExecutionMapper.ReportAggregate aggregate) {
        return new ReportDtos.Summary(
                aggregate.total().intValue(), aggregate.passed().intValue(), aggregate.failed().intValue(),
                aggregate.broken().intValue(), aggregate.skipped().intValue(), aggregate.executions().intValue());
    }

    private List<ReportDtos.Trend> trend(List<TestExecutionMapper.ReportTrend> rows) {
        return rows.stream()
                .map(row -> {
                    double passRate = row.getTotal() == 0 ? 0
                            : Math.round(row.getPassed() * 1000d / row.getTotal()) / 10d;
                    return new ReportDtos.Trend(row.getDay(), row.getTotal().intValue(), row.getPassed().intValue(),
                            row.getFailed().intValue(), row.getBroken().intValue(), row.getSkipped().intValue(), passRate);
                })
                .toList();
    }
}
