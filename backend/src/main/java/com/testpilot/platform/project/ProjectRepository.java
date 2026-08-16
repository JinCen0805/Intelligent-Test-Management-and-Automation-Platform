package com.testpilot.platform.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByDeletedFalseOrderByCreatedAtDesc();
    Optional<Project> findByIdAndDeletedFalse(Long id);
    Optional<Project> findByCodeAndDeletedFalse(String code);
    boolean existsByCodeAndDeletedFalse(String code);
    long countByDeletedFalse();
}
