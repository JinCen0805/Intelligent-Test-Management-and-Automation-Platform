package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationProjectRepository extends JpaRepository<AutomationProject, Long> {
    List<AutomationProject> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<AutomationProject> findByIdAndProjectId(Long id, Long projectId);
}
