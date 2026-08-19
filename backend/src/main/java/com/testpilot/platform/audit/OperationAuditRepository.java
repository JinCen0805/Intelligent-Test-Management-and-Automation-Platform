package com.testpilot.platform.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationAuditRepository extends JpaRepository<OperationAudit, Long> {
    List<OperationAudit> findAllByOrderByCreatedAtDesc();
}
