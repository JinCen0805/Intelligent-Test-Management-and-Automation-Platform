package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiAnalysisRecordRepository extends MybatisRepository<AiAnalysisRecord, Long> {

    public AiAnalysisRecordRepository(AiAnalysisRecordMapper mapper) {
        super(mapper, AiAnalysisRecord.class, AiAnalysisRecord::getId);
    }

    public Optional<AiAnalysisRecord> findByExecutionId(Long executionId) {
        return findOne(Wrappers.<AiAnalysisRecord>lambdaQuery().eq(AiAnalysisRecord::getExecutionId, executionId));
    }

    public void flush() {
        // MyBatis writes are immediate; kept for compatibility with JPA-era call sites.
    }
}
