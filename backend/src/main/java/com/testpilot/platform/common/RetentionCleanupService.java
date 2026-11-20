package com.testpilot.platform.common;

import com.testpilot.platform.ai.AiCallAuditRepository;
import com.testpilot.platform.audit.OperationAuditRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 审计类数据的保留策略：按配置的天数定期清理历史记录，避免审计表无限增长。
 */
@Service
public class RetentionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupService.class);

    private final OperationAuditRepository operationAuditRepository;
    private final AiCallAuditRepository aiCallAuditRepository;
    private final boolean enabled;
    private final int auditDays;
    private final int aiAuditDays;

    public RetentionCleanupService(
            OperationAuditRepository operationAuditRepository,
            AiCallAuditRepository aiCallAuditRepository,
            @Value("${testpilot.retention.enabled:true}") boolean enabled,
            @Value("${testpilot.retention.audit-days:90}") int auditDays,
            @Value("${testpilot.retention.ai-audit-days:180}") int aiAuditDays
    ) {
        this.operationAuditRepository = operationAuditRepository;
        this.aiCallAuditRepository = aiCallAuditRepository;
        this.enabled = enabled;
        this.auditDays = Math.max(1, auditDays);
        this.aiAuditDays = Math.max(1, aiAuditDays);
    }

    @Scheduled(
            fixedDelayString = "${testpilot.retention.cleanup-delay-ms:86400000}",
            initialDelayString = "${testpilot.retention.cleanup-initial-delay-ms:60000}"
    )
    public void scheduledCleanup() {
        CleanupResult result = cleanupNow();
        if (result.operationAudits() > 0 || result.aiCallAudits() > 0) {
            log.info("审计保留策略清理完成: operationAudits={}, aiCallAudits={}",
                    result.operationAudits(), result.aiCallAudits());
        }
    }

    public CleanupResult cleanupNow() {
        if (!enabled) {
            return new CleanupResult(0L, 0L);
        }
        LocalDateTime now = LocalDateTime.now();
        long operationDeleted = operationAuditRepository.deleteByCreatedAtBefore(now.minusDays(auditDays));
        long aiAuditDeleted = aiCallAuditRepository.deleteByCreatedAtBefore(now.minusDays(aiAuditDays));
        return new CleanupResult(operationDeleted, aiAuditDeleted);
    }

    public record CleanupResult(long operationAudits, long aiCallAudits) {
    }
}
