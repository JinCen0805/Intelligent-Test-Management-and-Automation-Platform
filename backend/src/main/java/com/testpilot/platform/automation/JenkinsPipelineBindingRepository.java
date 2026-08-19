package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JenkinsPipelineBindingRepository extends JpaRepository<JenkinsPipelineBinding, Long> {
    List<JenkinsPipelineBinding> findAllByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(
            Long projectId, Long automationProjectId);
    Optional<JenkinsPipelineBinding> findFirstByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(
            Long projectId, Long automationProjectId);
    Optional<JenkinsPipelineBinding> findByIdAndProjectId(Long id, Long projectId);
    Optional<JenkinsPipelineBinding> findFirstByAutomationProjectIdOrderByCreatedAtAsc(Long automationProjectId);
    Optional<JenkinsPipelineBinding> findByProjectIdAndAutomationProjectIdAndJenkinsInstanceIdAndJobName(
            Long projectId, Long automationProjectId, Long jenkinsInstanceId, String jobName);
}
