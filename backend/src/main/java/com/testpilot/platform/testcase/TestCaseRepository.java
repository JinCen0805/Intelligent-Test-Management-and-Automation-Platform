package com.testpilot.platform.testcase;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    @Query("""
            select c from TestCase c
            where c.projectId = :projectId
              and (:status is null or :status = '' or c.status = :status)
              and (:keyword is null or :keyword = ''
                   or lower(c.caseNo) like lower(concat('%', :keyword, '%'))
                   or lower(c.title) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(c.moduleName, '')) like lower(concat('%', :keyword, '%')))
              and (:priority is null or :priority = '' or c.priority = :priority)
              and (:caseType is null or :caseType = '' or c.caseType = :caseType)
            """)
    Page<TestCase> search(
            @Param("projectId") Long projectId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("priority") String priority,
            @Param("caseType") String caseType,
            Pageable pageable
    );

    List<TestCase> findTop200ByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<TestCase> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndCaseNo(Long projectId, String caseNo);

    long countByProjectIdAndRequirementId(Long projectId, Long requirementId);

    long countByProjectId(Long projectId);
}
