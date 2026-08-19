package com.testpilot.platform.api;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiEnvironmentVariableRepository extends JpaRepository<ApiEnvironmentVariable, Long> {
    List<ApiEnvironmentVariable> findAllByProjectIdAndEnvironmentOrderByNameAsc(Long projectId, String environment);
    Optional<ApiEnvironmentVariable> findByIdAndProjectId(Long id, Long projectId);
    Optional<ApiEnvironmentVariable> findByProjectIdAndEnvironmentAndName(Long projectId, String environment, String name);
}
