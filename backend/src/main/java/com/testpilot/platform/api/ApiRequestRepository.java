package com.testpilot.platform.api;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {
    List<ApiRequest> findAllByCollectionIdOrderByCreatedAtAsc(Long collectionId);
    List<ApiRequest> findAllByProjectId(Long projectId);
    Optional<ApiRequest> findByIdAndProjectId(Long id, Long projectId);
}
