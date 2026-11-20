package com.testpilot.platform.audit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class OperationAuditRepository extends MybatisRepository<OperationAudit, Long> {

    public OperationAuditRepository(OperationAuditMapper mapper) {
        super(mapper, OperationAudit.class, OperationAudit::getId);
    }

    public List<OperationAudit> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<OperationAudit>lambdaQuery().orderByDesc(OperationAudit::getCreatedAt));
    }

    public Page<OperationAudit> findPageByOrderByCreatedAtDesc(Pageable pageable) {
        return page(pageable, Wrappers.<OperationAudit>lambdaQuery().orderByDesc(OperationAudit::getCreatedAt));
    }

    /** 保留策略：删除早于 cutoff 的审计记录，返回删除行数。 */
    public long deleteByCreatedAtBefore(LocalDateTime cutoff) {
        return getMapper().delete(Wrappers.<OperationAudit>lambdaQuery()
                .lt(OperationAudit::getCreatedAt, cutoff));
    }
}
