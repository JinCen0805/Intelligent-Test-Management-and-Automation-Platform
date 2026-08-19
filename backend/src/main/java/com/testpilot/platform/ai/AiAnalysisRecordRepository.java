package com.testpilot.platform.ai;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRecordRepository extends JpaRepository<AiAnalysisRecord, Long> {
    Optional<AiAnalysisRecord> findByExecutionId(Long executionId);
}
