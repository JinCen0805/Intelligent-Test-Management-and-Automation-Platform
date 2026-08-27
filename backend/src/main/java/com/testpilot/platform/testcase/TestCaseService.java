package com.testpilot.platform.testcase;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.requirement.RequirementRepository;
import com.testpilot.platform.user.User;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    private final OperationAuditService operationAuditService;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            RequirementRepository requirementRepository,
            ProjectAccessService projectAccessService,
            OperationAuditService operationAuditService
    ) {
        this.testCaseRepository = testCaseRepository;
        this.requirementRepository = requirementRepository;
        this.projectAccessService = projectAccessService;
        this.operationAuditService = operationAuditService;
    }

    public Page<TestCaseDtos.Response> list(
            Long projectId,
            String status,
            String keyword,
            String priority,
            String caseType,
            String source,
            String suiteType,
            int page,
            int size,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_READ);
        validateFilters(priority, caseType, suiteType);
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
                        normalizeFilter(source),
                        normalizeFilter(suiteType),
                        pageable
                )
                .map(TestCaseDtos.Response::from);
    }

    public List<TestCaseDtos.Response> export(
            Long projectId,
            String status,
            String keyword,
            String priority,
            String caseType,
            String source,
            String suiteType,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_READ);
        validateFilters(priority, caseType, suiteType);
        return testCaseRepository.searchAll(
                        projectId,
                        normalizeFilter(status),
                        normalizeFilter(keyword),
                        normalizeFilter(priority),
                        normalizeFilter(caseType),
                        normalizeFilter(source),
                        normalizeFilter(suiteType)
                )
                .stream()
                .map(TestCaseDtos.Response::from)
                .toList();
    }

    public byte[] exportToExcel(List<TestCaseDtos.Response> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("测试用例");
            String[] headers = {
                    "用例编号", "标题", "功能模块", "前置条件", "测试步骤",
                    "测试数据", "预期结果", "优先级", "测试范围", "类型", "来源", "状态", "标签", "更新时间"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int index = 1;
            for (TestCaseDtos.Response rowData : rows) {
                Row row = sheet.createRow(index++);
                write(row, 0, rowData.caseNo(), wrapStyle);
                write(row, 1, rowData.title(), wrapStyle);
                write(row, 2, rowData.moduleName(), wrapStyle);
                write(row, 3, rowData.precondition(), wrapStyle);
                write(row, 4, rowData.steps(), wrapStyle);
                write(row, 5, rowData.testData(), wrapStyle);
                write(row, 6, rowData.expectedResult(), wrapStyle);
                write(row, 7, rowData.priority(), wrapStyle);
                write(row, 8, suiteTypeLabel(rowData.suiteType()), wrapStyle);
                write(row, 9, caseTypeLabel(rowData.caseType()), wrapStyle);
                write(row, 10, rowData.source(), wrapStyle);
                write(row, 11, statusLabel(rowData.status()), wrapStyle);
                write(row, 12, rowData.tags(), wrapStyle);
                write(row, 13, rowData.updatedAt() == null ? "" : rowData.updatedAt().toString(), wrapStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 18_000));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException(50050, "测试用例导出失败");
        }
    }

    public TestCaseDtos.Response get(Long projectId, Long testCaseId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_READ);
        return TestCaseDtos.Response.from(findCase(projectId, testCaseId));
    }

    @Transactional
    public TestCaseDtos.Response create(
            Long projectId,
            TestCaseDtos.UpsertRequest request,
            User currentUser,
            String source
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_WRITE);
        ensureRequirement(projectId, request.requirementId());
        String suiteType = normalizeSuiteType(request.suiteType());
        String caseNo = request.caseNo() == null || request.caseNo().isBlank()
                ? nextCaseNo(projectId, suiteType)
                : request.caseNo().trim().toUpperCase(Locale.ROOT);
        if (testCaseRepository.existsByProjectIdAndSuiteTypeAndCaseNo(projectId, suiteType, caseNo)) {
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
                suiteType,
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
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_WRITE);
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
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_REVIEW);
        TestCase testCase = findCase(projectId, testCaseId);
        String targetStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!isAllowedTransition(testCase.getStatus(), targetStatus, testCase.getSource())) {
            throw new BusinessException(40005, "测试用例状态流转不合法");
        }
        String previousStatus = testCase.getStatus();
        testCase.setStatus(targetStatus);
        TestCase saved = testCaseRepository.save(testCase);
        operationAuditService.record(projectId, currentUser, "TESTCASE_REVIEW", "TESTCASE", testCaseId,
                "用例 " + saved.getCaseNo() + " 状态从 " + previousStatus + " 流转为 " + targetStatus, true);
        return TestCaseDtos.Response.from(saved);
    }

    @Transactional
    public void delete(Long projectId, Long testCaseId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.TESTCASE_WRITE);
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

    private String nextCaseNo(Long projectId, String suiteType) {
        long next = testCaseRepository.countByProjectIdAndSuiteType(projectId, suiteType) + 1;
        return "TC-" + String.format("%04d", next);
    }

    private boolean isAllowedTransition(String currentStatus, String targetStatus, String source) {
        if (currentStatus.equals(targetStatus)) {
            return true;
        }
        // AI 用例在前端只有“通过/删除”，不经过单独的 Review 按钮。
        if ("AI".equalsIgnoreCase(source)
                && "DRAFT".equals(currentStatus)
                && "APPROVED".equals(targetStatus)) {
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

    private void write(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "DRAFT" -> "草稿";
            case "REVIEW" -> "待 Review";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已退回";
            default -> status;
        };
    }

    private String caseTypeLabel(String caseType) {
        return switch (caseType == null ? "" : caseType.toUpperCase(Locale.ROOT)) {
            case "FUNCTIONAL" -> "功能";
            case "EXCEPTION" -> "异常";
            case "BOUNDARY" -> "边界";
            default -> caseType;
        };
    }

    private String suiteTypeLabel(String suiteType) {
        return "API".equalsIgnoreCase(suiteType) ? "接口测试" : "功能测试";
    }

    private void validateFilters(String priority, String caseType, String suiteType) {
        if (priority != null && !priority.isBlank()
                && !java.util.Set.of("P0", "P1", "P2", "P3").contains(normalizeFilter(priority))) {
            throw new BusinessException(40011, "优先级筛选值不合法");
        }
        if (caseType != null && !caseType.isBlank()
                && !java.util.Set.of("FUNCTIONAL", "EXCEPTION", "BOUNDARY").contains(normalizeFilter(caseType))) {
            throw new BusinessException(40012, "测试类型筛选值不合法");
        }
        if (suiteType != null && !suiteType.isBlank()
                && !java.util.Set.of("FUNCTIONAL", "API").contains(normalizeFilter(suiteType))) {
            throw new BusinessException(40013, "测试套件类型筛选值不合法");
        }
    }

    private String normalizeSuiteType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return java.util.Set.of("FUNCTIONAL", "API").contains(normalized) ? normalized : "FUNCTIONAL";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
