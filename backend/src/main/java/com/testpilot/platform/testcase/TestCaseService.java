package com.testpilot.platform.testcase;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.requirement.RequirementRepository;
import com.testpilot.platform.user.User;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final RequirementRepository requirementRepository;
    private final ProjectAccessService projectAccessService;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            RequirementRepository requirementRepository,
            ProjectAccessService projectAccessService
    ) {
        this.testCaseRepository = testCaseRepository;
        this.requirementRepository = requirementRepository;
        this.projectAccessService = projectAccessService;
    }

    public Page<TestCaseDtos.Response> list(
            Long projectId,
            String status,
            String keyword,
            String priority,
            String caseType,
            int page,
            int size,
            User currentUser
    ) {
        projectAccessService.requireRead(projectId, currentUser);
        validateFilters(priority, caseType);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return testCaseRepository
                .search(
                        projectId,
                        normalizeFilter(status),
                        normalizeFilter(keyword),
                        normalizeFilter(priority),
                        normalizeFilter(caseType),
                        pageable
                )
                .map(TestCaseDtos.Response::from);
    }

    public TestCaseDtos.Response get(Long projectId, Long testCaseId, User currentUser) {
        projectAccessService.requireRead(projectId, currentUser);
        return TestCaseDtos.Response.from(findCase(projectId, testCaseId));
    }

    @Transactional
    public TestCaseDtos.Response create(
            Long projectId,
            TestCaseDtos.UpsertRequest request,
            User currentUser,
            String source
    ) {
        projectAccessService.requireRead(projectId, currentUser);
        ensureRequirement(projectId, request.requirementId());
        String caseNo = request.caseNo() == null || request.caseNo().isBlank()
                ? nextCaseNo(projectId)
                : request.caseNo().trim().toUpperCase(Locale.ROOT);
        if (testCaseRepository.existsByProjectIdAndCaseNo(projectId, caseNo)) {
            throw new BusinessException(40902, "测试用例编号已存在");
        }
        TestCase testCase = new TestCase(
                projectId,
                request.requirementId(),
                caseNo,
                request.title().trim(),
                blankToNull(request.moduleName()),
                request.precondition(),
                request.steps(),
                request.testData(),
                request.expectedResult(),
                request.priority().trim().toUpperCase(Locale.ROOT),
                request.caseType().trim().toUpperCase(Locale.ROOT),
                request.tags(),
                "DRAFT",
                source,
                currentUser.getId()
        );
        return TestCaseDtos.Response.from(testCaseRepository.save(testCase));
    }

    @Transactional
    public TestCaseDtos.Response update(
            Long projectId,
            Long testCaseId,
            TestCaseDtos.UpsertRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        ensureRequirement(projectId, request.requirementId());
        TestCase testCase = findCase(projectId, testCaseId);
        testCase.updateFrom(request);
        return TestCaseDtos.Response.from(testCaseRepository.save(testCase));
    }

    @Transactional
    public TestCaseDtos.Response changeStatus(
            Long projectId,
            Long testCaseId,
            String status,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        TestCase testCase = findCase(projectId, testCaseId);
        String targetStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!isAllowedTransition(testCase.getStatus(), targetStatus)) {
            throw new BusinessException(40005, "测试用例状态流转不合法");
        }
        testCase.setStatus(targetStatus);
        return TestCaseDtos.Response.from(testCaseRepository.save(testCase));
    }

    @Transactional
    public void delete(Long projectId, Long testCaseId, User currentUser) {
        projectAccessService.requireWrite(projectId, currentUser);
        testCaseRepository.delete(findCase(projectId, testCaseId));
    }

    private TestCase findCase(Long projectId, Long testCaseId) {
        return testCaseRepository.findByIdAndProjectId(testCaseId, projectId)
                .orElseThrow(() -> new BusinessException(40403, "测试用例不存在"));
    }

    private void ensureRequirement(Long projectId, Long requirementId) {
        if (requirementId != null && requirementRepository.findByIdAndProjectId(requirementId, projectId).isEmpty()) {
            throw new BusinessException(40402, "需求文档不存在");
        }
    }

    private String nextCaseNo(Long projectId) {
        long next = testCaseRepository.countByProjectId(projectId) + 1;
        return "TC-" + String.format("%04d", next);
    }

    private boolean isAllowedTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equals(targetStatus)) {
            return true;
        }
        return switch (currentStatus) {
            case "DRAFT" -> targetStatus.equals("REVIEW");
            case "REVIEW" -> targetStatus.equals("APPROVED") || targetStatus.equals("REJECTED") || targetStatus.equals("DRAFT");
            case "REJECTED" -> targetStatus.equals("DRAFT") || targetStatus.equals("REVIEW");
            case "APPROVED" -> false;
            default -> false;
        };
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateFilters(String priority, String caseType) {
        if (priority != null && !priority.isBlank()
                && !java.util.Set.of("P0", "P1", "P2", "P3").contains(normalizeFilter(priority))) {
            throw new BusinessException(40011, "优先级筛选值不合法");
        }
        if (caseType != null && !caseType.isBlank()
                && !java.util.Set.of("FUNCTIONAL", "EXCEPTION", "BOUNDARY").contains(normalizeFilter(caseType))) {
            throw new BusinessException(40012, "测试类型筛选值不合法");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
