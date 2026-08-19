package com.testpilot.platform.ai;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAutomationTaskDraftRepository extends JpaRepository<AiAutomationTaskDraft, Long> {
    Optional<AiAutomationTaskDraft> findByIdAndProjectId(Long id, Long projectId);
    List<AiAutomationTaskDraft> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
}
