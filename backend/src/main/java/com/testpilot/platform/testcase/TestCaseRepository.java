package com.testpilot.platform.testcase;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class TestCaseRepository extends MybatisRepository<TestCase, Long> {

    public TestCaseRepository(TestCaseMapper mapper) {
        super(mapper, TestCase.class, TestCase::getId);
    }

    public Page<TestCase> search(
            Long projectId,
            String status,
            String keyword,
            String priority,
            String caseType,
            String source,
            String suiteType,
            Pageable pageable
    ) {
        var wrapper = Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .eq(status != null && !status.isBlank(), TestCase::getStatus, status)
                .eq(priority != null && !priority.isBlank(), TestCase::getPriority, priority)
                .eq(caseType != null && !caseType.isBlank(), TestCase::getCaseType, caseType)
                .eq(source != null && !source.isBlank(), TestCase::getSource, source)
                .eq(suiteType != null && !suiteType.isBlank(), TestCase::getSuiteType, suiteType);
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w.apply("lower(case_no) like lower({0})", like)
                    .or()
                    .apply("lower(title) like lower({0})", like)
                    .or()
                    .apply("lower(coalesce(module_name, '')) like lower({0})", like));
        }
        wrapper.orderByDesc(TestCase::getCreatedAt);
        return page(pageable, wrapper);
    }

    public List<TestCase> searchAll(
            Long projectId,
            String status,
            String keyword,
            String priority,
            String caseType,
            String source,
            String suiteType
    ) {
        return list(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .eq(status != null && !status.isBlank(), TestCase::getStatus, status)
                .eq(priority != null && !priority.isBlank(), TestCase::getPriority, priority)
                .eq(caseType != null && !caseType.isBlank(), TestCase::getCaseType, caseType)
                .eq(source != null && !source.isBlank(), TestCase::getSource, source)
                .eq(suiteType != null && !suiteType.isBlank(), TestCase::getSuiteType, suiteType)
                .and(keyword != null && !keyword.isBlank(), w -> {
                    String like = "%" + keyword + "%";
                    w.apply("lower(case_no) like lower({0})", like)
                            .or()
                            .apply("lower(title) like lower({0})", like)
                            .or()
                            .apply("lower(coalesce(module_name, '')) like lower({0})", like);
                })
                .orderByDesc(TestCase::getCreatedAt));
    }

    public List<TestCase> findTop200ByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return list(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .orderByDesc(TestCase::getCreatedAt)
                .last("limit 200"));
    }

    public List<TestCase> findTop200ByProjectIdAndSuiteTypeOrderByCreatedAtDesc(Long projectId, String suiteType) {
        return list(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .eq(TestCase::getSuiteType, suiteType)
                .orderByDesc(TestCase::getCreatedAt)
                .last("limit 200"));
    }

    public Optional<TestCase> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getId, id)
                .eq(TestCase::getProjectId, projectId));
    }

    public boolean existsByProjectIdAndSuiteTypeAndCaseNo(Long projectId, String suiteType, String caseNo) {
        return exists(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .eq(TestCase::getSuiteType, suiteType)
                .eq(TestCase::getCaseNo, caseNo));
    }

    public long countByProjectIdAndRequirementId(Long projectId, Long requirementId) {
        return count(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .eq(TestCase::getRequirementId, requirementId));
    }

    public long countByProjectId(Long projectId) {
        return count(Wrappers.<TestCase>lambdaQuery().eq(TestCase::getProjectId, projectId));
    }

    public long countByProjectIdAndSuiteType(Long projectId, String suiteType) {
        return count(Wrappers.<TestCase>lambdaQuery()
                .eq(TestCase::getProjectId, projectId)
                .eq(TestCase::getSuiteType, suiteType));
    }
}
