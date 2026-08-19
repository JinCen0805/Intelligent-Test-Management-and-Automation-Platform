package com.testpilot.platform.api;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiExecutionHistoryRepository extends JpaRepository<ApiExecutionHistory, Long> {
    List<ApiExecutionHistory> findAllByProjectIdOrderByStartedAtDesc(Long projectId);
    List<ApiExecutionHistory> findAllByRequestId(Long requestId);
    Page<ApiExecutionHistory> findAllByProjectIdOrderByStartedAtDesc(Long projectId, Pageable pageable);
}
