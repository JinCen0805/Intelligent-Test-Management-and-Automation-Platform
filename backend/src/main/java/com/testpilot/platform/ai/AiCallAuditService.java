package com.testpilot.platform.ai;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 调用审计写入与查询。
 *
 * <p>写入使用 {@code REQUIRES_NEW} 独立事务，保证即使主流程回滚（例如 AI 调用失败后抛出异常），
 * 审计记录仍然落库；写入失败也仅吞掉异常，绝不影响主业务。
 */
@Service
public class AiCallAuditService {

    private final AiCallAuditRepository repository;
    private final RbacService rbacService;

    public AiCallAuditService(AiCallAuditRepository repository, RbacService rbacService) {
        this.repository = repository;
        this.rbacService = rbacService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Long projectId,
            User user,
            String taskType,
            AiModel model,
            Long promptId,
            String promptName,
            String promptVersion,
            boolean success,
            Integer statusCode,
            String errorMessage,
            long latencyMs
    ) {
        try {
            repository.save(new AiCallAudit(
                    projectId,
                    user == null ? null : user.getId(),
                    user == null ? null : user.getUsername(),
                    taskType,
                    model == null ? null : model.getId(),
                    model == null ? null : model.getName(),
                    model == null ? null : model.getProvider(),
                    promptId,
                    promptName,
                    promptVersion,
                    success ? "SUCCESS" : "FAILED",
                    statusCode,
                    truncate(errorMessage, 4000),
                    latencyMs
            ));
        } catch (Exception ignored) {
            // 审计写入失败不能影响主流程
        }
    }

    @Transactional(readOnly = true)
    public List<AiDtos.AiCallAuditResponse> list(User user) {
        requireAdmin(user);
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(AiDtos.AiCallAuditResponse::from)
                .toList();
    }

    private void requireAdmin(User user) {
        if (user == null || !rbacService.hasSystemPermission(user.getId(), RbacPermissions.SYSTEM_AUDIT_READ)) {
            throw new AccessDeniedException("只有管理员可以查看 AI 调用审计");
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
