package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AiCallAuditRepository extends MybatisRepository<AiCallAudit, Long> {

    public AiCallAuditRepository(AiCallAuditMapper mapper) {
        super(mapper, AiCallAudit.class, AiCallAudit::getId);
    }

    public List<AiCallAudit> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<AiCallAudit>lambdaQuery().orderByDesc(AiCallAudit::getCreatedAt));
    }
}
