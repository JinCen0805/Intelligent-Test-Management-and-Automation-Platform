package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class AiCallAuditRepository extends MybatisRepository<AiCallAudit, Long> {

    public AiCallAuditRepository(AiCallAuditMapper mapper) {
        super(mapper, AiCallAudit.class, AiCallAudit::getId);
    }

    public List<AiCallAudit> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<AiCallAudit>lambdaQuery().orderByDesc(AiCallAudit::getCreatedAt));
    }

    public Page<AiCallAudit> findPageByOrderByCreatedAtDesc(Pageable pageable) {
        return page(pageable, Wrappers.<AiCallAudit>lambdaQuery().orderByDesc(AiCallAudit::getCreatedAt));
    }

    /** 保留策略：删除早于 cutoff 的 AI 调用审计记录，返回删除行数。 */
    public long deleteByCreatedAtBefore(LocalDateTime cutoff) {
        return getMapper().delete(Wrappers.<AiCallAudit>lambdaQuery()
                .lt(AiCallAudit::getCreatedAt, cutoff));
    }
}
