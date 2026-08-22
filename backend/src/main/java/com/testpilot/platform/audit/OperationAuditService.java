package com.testpilot.platform.audit;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作审计写入与查询。
 *
 * <p>写入使用 {@code REQUIRES_NEW} 独立事务，保证即使主流程回滚（例如校验失败抛出异常），
 * 审计记录仍然落库；写入失败也仅吞掉异常，绝不影响主业务。
 */
@Service
public class OperationAuditService {

    private final OperationAuditRepository repository;

    public OperationAuditService(OperationAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Long projectId,
            User user,
            String action,
            String targetType,
            Long targetId,
            String detail,
            boolean success
    ) {
        try {
            repository.save(new OperationAudit(
                    projectId,
                    user == null ? null : user.getId(),
                    user == null ? null : user.getUsername(),
                    action,
                    targetType,
                    targetId,
                    truncate(detail, 4000),
                    success ? "SUCCESS" : "FAILED"
            ));
        } catch (Exception ignored) {
            // 审计写入失败不能影响主流程
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:audit:read')")
    public List<OperationAuditDtos.OperationAuditResponse> list(User user) {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(OperationAuditDtos.OperationAuditResponse::from)
                .toList();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
