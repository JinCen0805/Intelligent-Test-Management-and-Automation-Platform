package com.testpilot.platform.api;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCollectionRepository extends JpaRepository<ApiCollection, Long> {
    List<ApiCollection> findAllByProjectIdOrderByCreatedAtAsc(Long projectId);
    Optional<ApiCollection> findByIdAndProjectId(Long id, Long projectId);
}
