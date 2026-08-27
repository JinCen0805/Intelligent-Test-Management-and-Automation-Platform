package com.testpilot.platform.testcase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;

public final class TestCaseDtos {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            @Pattern(regexp = "FUNCTIONAL|API|functional|api", message = "必须是 FUNCTIONAL/API")
            String suiteType,
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
            String suiteType,
            String tags,
            Map<String, Object> extraFields,
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
                    testCase.getSuiteType(),
                    testCase.getTags(),
                    parseExtraFields(testCase.getExtraFieldsJson()),
                    testCase.getStatus(),
                    testCase.getSource(),
                    testCase.getCreatedBy(),
                    testCase.getCreatedAt(),
                    testCase.getUpdatedAt()
            );
        }

        private static Map<String, Object> parseExtraFields(String value) {
            if (value == null || value.isBlank()) {
                return Map.of();
            }
            try {
                Map<String, Object> parsed = OBJECT_MAPPER.readValue(value, new TypeReference<>() {});
                return parsed == null ? Map.of() : parsed;
            } catch (Exception ignored) {
                return Map.of();
            }
        }
    }
}
