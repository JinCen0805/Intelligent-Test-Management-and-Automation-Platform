package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<TestExecution> findAllByStatusIn(List<String> statuses);
    List<TestExecution> findAllByProjectIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            LocalDateTime from,
            LocalDateTime to
    );
    List<TestExecution> findAllByProjectIdInAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            String status,
            LocalDateTime from,
            LocalDateTime to
    );
    Page<TestExecution> findAllByProjectIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
    Page<TestExecution> findAllByProjectIdInAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> projectIds,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
    Optional<TestExecution> findByIdAndProjectId(Long id, Long projectId);
}
