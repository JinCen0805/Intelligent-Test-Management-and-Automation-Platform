package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestTaskRepository extends JpaRepository<TestTask, Long> {
    List<TestTask> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<TestTask> findByIdAndProjectId(Long id, Long projectId);
}
