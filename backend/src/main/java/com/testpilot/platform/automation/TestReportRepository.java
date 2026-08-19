package com.testpilot.platform.automation;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestReportRepository extends JpaRepository<TestReport, Long> {
    Optional<TestReport> findByExecutionId(Long executionId);
    List<TestReport> findAllByExecutionIdIn(List<Long> executionIds);
}
