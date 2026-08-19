package com.testpilot.platform.ai;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findAllByOrderByCreatedAtDesc();
    Optional<PromptTemplate> findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc(String taskType);
    List<PromptTemplate> findByNameAndTaskType(String name, String taskType);
}
