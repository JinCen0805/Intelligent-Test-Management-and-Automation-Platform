package com.testpilot.platform.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class TestCaseDtos {

    private TestCaseDtos() {
    }

    public record UpsertRequest(
            Long requirementId,
            @Size(max = 64, message = "长度不能超过 64 个字符")
            String caseNo,
            @NotBlank(message = "不能为空")
            @Size(max = 255, message = "长度不能超过 255 个字符")
            String title,
            @Size(max = 128, message = "长度不能超过 128 个字符")
            String moduleName,
            String precondition,
            String steps,
            String testData,
            String expectedResult,
            @NotBlank(message = "不能为空")
            @Pattern(regexp = "P0|P1|P2|P3|p0|p1|p2|p3", message = "必须是 P0/P1/P2/P3")
            String priority,
            @NotBlank(message = "不能为空")
            @Size(max = 32, message = "长度不能超过 32 个字符")
            String caseType,
            @Size(max = 500, message = "长度不能超过 500 个字符")
            String tags
    ) {
    }

    public record StatusRequest(
            @NotBlank(message = "不能为空")
            @Pattern(regexp = "DRAFT|REVIEW|APPROVED|REJECTED", message = "状态不合法")
            String status
    ) {
    }

    public record Response(
            Long id,
            Long projectId,
            Long requirementId,
            String caseNo,
            String title,
            String moduleName,
            String precondition,
            String steps,
            String testData,
            String expectedResult,
            String priority,
            String caseType,
            String tags,
            String status,
            String source,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(TestCase testCase) {
            return new Response(
                    testCase.getId(),
                    testCase.getProjectId(),
                    testCase.getRequirementId(),
                    testCase.getCaseNo(),
                    testCase.getTitle(),
                    testCase.getModuleName(),
                    testCase.getPrecondition(),
                    testCase.getSteps(),
                    testCase.getTestData(),
                    testCase.getExpectedResult(),
                    testCase.getPriority(),
                    testCase.getCaseType(),
                    testCase.getTags(),
                    testCase.getStatus(),
                    testCase.getSource(),
                    testCase.getCreatedBy(),
                    testCase.getCreatedAt(),
                    testCase.getUpdatedAt()
            );
        }
    }
}
