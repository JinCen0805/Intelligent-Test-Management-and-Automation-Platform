package com.testpilot.platform.requirement;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    List<Requirement> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<Requirement> findByIdAndProjectId(Long id, Long projectId);
}
