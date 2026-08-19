package com.testpilot.platform.audit;

import java.time.LocalDateTime;

public final class OperationAuditDtos {

    private OperationAuditDtos() {
    }

    public record OperationAuditResponse(
            Long id,
            Long projectId,
            Long userId,
            String userName,
            String action,
            String targetType,
            Long targetId,
            String detail,
            String result,
            LocalDateTime createdAt
    ) {
        public static OperationAuditResponse from(OperationAudit audit) {
            return new OperationAuditResponse(
                    audit.getId(),
                    audit.getProjectId(),
                    audit.getUserId(),
                    audit.getUserName(),
                    audit.getAction(),
                    audit.getTargetType(),
                    audit.getTargetId(),
                    audit.getDetail(),
                    audit.getResult(),
                    audit.getCreatedAt()
            );
        }
    }
}
