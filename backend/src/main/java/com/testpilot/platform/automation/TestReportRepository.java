package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TestReportRepository extends MybatisRepository<TestReport, Long> {

    public TestReportRepository(TestReportMapper mapper) {
        super(mapper, TestReport.class, TestReport::getId);
    }

    public Optional<TestReport> findByExecutionId(Long executionId) {
        return findOne(Wrappers.<TestReport>lambdaQuery().eq(TestReport::getExecutionId, executionId));
    }

    public List<TestReport> findAllByExecutionIdIn(List<Long> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            return List.of();
        }
        return list(Wrappers.<TestReport>lambdaQuery().in(TestReport::getExecutionId, executionIds));
    }

    public void flush() {
        // MyBatis writes are immediate; kept for compatibility with JPA-era call sites.
    }
}
