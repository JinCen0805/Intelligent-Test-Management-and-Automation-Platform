package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JenkinsInstanceRepository extends JpaRepository<JenkinsInstance, Long> {
    List<JenkinsInstance> findAllByOrderByCreatedAtDesc();
    Optional<JenkinsInstance> findByIdAndStatus(Long id, String status);
}
