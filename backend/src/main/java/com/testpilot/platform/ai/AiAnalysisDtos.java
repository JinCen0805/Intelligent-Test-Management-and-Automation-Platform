package com.testpilot.platform.ai;

import java.time.LocalDateTime;
import java.util.List;

public final class AiAnalysisDtos {

    private AiAnalysisDtos() {
    }

    public record AnalyzeRequest(
            Long modelId,
            Long promptId
    ) {
    }

    public record AnalysisResponse(
            Long executionId,
            Long projectId,
            String projectName,
            Long testTaskId,
            String taskName,
            String buildNumber,
            String branch,
            String commitSha,
            String environment,
            String status,
            String category,
            String categoryLabel,
            List<String> possibleCauses,
            List<String> relatedErrors,
            List<String> checkLocations,
            List<String> fixDirections,
            List<String> riskWarnings,
            String modelName,
            String provider,
            String promptName,
            String promptVersion,
            LocalDateTime analyzedAt,
            String disclaimer,
            String errorMessage
    ) {
    }
}
