package com.testpilot.platform.audit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
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
}
