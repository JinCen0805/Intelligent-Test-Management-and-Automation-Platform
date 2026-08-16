package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<TestExecution> findByIdAndProjectId(Long id, Long projectId);
}
