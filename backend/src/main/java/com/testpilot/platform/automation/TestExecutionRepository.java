package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class TestExecutionRepository extends MybatisRepository<TestExecution, Long> {

    public TestExecutionRepository(TestExecutionMapper mapper) {
        super(mapper, TestExecution.class, TestExecution::getId);
    }

    public List<TestExecution> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return list(Wrappers.<TestExecution>lambdaQuery()
                .eq(TestExecution::getProjectId, projectId)
                .orderByDesc(TestExecution::getCreatedAt));
    }

    public List<TestExecution> findAllByStatusIn(List<String> statuses) {
        return statuses == null || statuses.isEmpty()
                ? List.of()
                : list(Wrappers.<TestExecution>lambdaQuery().in(TestExecution::getStatus, statuses));
    }

    public List<TestExecution> findAllByProjectIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return list(Wrappers.<TestExecution>lambdaQuery()
                .in(TestExecution::getProjectId, projectIds)
                .between(TestExecution::getCreatedAt, from, to)
                .orderByDesc(TestExecution::getCreatedAt));
    }

    public List<TestExecution> findAllByProjectIdInAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            String status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return list(Wrappers.<TestExecution>lambdaQuery()
                .in(TestExecution::getProjectId, projectIds)
                .eq(TestExecution::getStatus, status)
                .between(TestExecution::getCreatedAt, from, to)
                .orderByDesc(TestExecution::getCreatedAt));
    }

    public Page<TestExecution> findAllByProjectIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return page(pageable, Wrappers.<TestExecution>lambdaQuery()
                .in(TestExecution::getProjectId, projectIds)
                .between(TestExecution::getCreatedAt, from, to)
                .orderByDesc(TestExecution::getCreatedAt));
    }

    public Page<TestExecution> findAllByProjectIdInAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return page(pageable, Wrappers.<TestExecution>lambdaQuery()
                .in(TestExecution::getProjectId, projectIds)
                .eq(TestExecution::getStatus, status)
                .between(TestExecution::getCreatedAt, from, to)
                .orderByDesc(TestExecution::getCreatedAt));
    }

    public Optional<TestExecution> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<TestExecution>lambdaQuery()
                .eq(TestExecution::getId, id)
                .eq(TestExecution::getProjectId, projectId));
    }

    public boolean existsActiveByTaskId(Long projectId, Long taskId) {
        return exists(Wrappers.<TestExecution>lambdaQuery()
                .eq(TestExecution::getProjectId, projectId)
                .eq(TestExecution::getTestTaskId, taskId)
                .in(TestExecution::getStatus, List.of("QUEUED", "RUNNING")));
    }

    public Optional<TestExecution> findLatestActiveByTaskId(Long projectId, Long taskId) {
        return findFirst(Wrappers.<TestExecution>lambdaQuery()
                .eq(TestExecution::getProjectId, projectId)
                .eq(TestExecution::getTestTaskId, taskId)
                .in(TestExecution::getStatus, List.of("QUEUED", "RUNNING"))
                .orderByDesc(TestExecution::getCreatedAt));
    }

    public TestExecutionMapper.ReportAggregate aggregateReports(List<Long> projectIds, String status,
                                                                 LocalDateTime from, LocalDateTime to) {
        return ((TestExecutionMapper) getMapper()).aggregateReports(projectIds, status, from, to);
    }

    public List<TestExecutionMapper.ReportTrend> trendReports(List<Long> projectIds, String status,
                                                               LocalDateTime from, LocalDateTime to) {
        return ((TestExecutionMapper) getMapper()).trendReports(projectIds, status, from, to);
    }
}
