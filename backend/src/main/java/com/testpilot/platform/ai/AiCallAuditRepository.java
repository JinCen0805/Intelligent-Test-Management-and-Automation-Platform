package com.testpilot.platform.ai;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCallAuditRepository extends JpaRepository<AiCallAudit, Long> {
    List<AiCallAudit> findAllByOrderByCreatedAtDesc();
}
