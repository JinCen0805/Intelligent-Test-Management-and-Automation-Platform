package com.testpilot.platform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AiTaskDtos {

    private AiTaskDtos() {
    }

    public record SubmitResponse(String taskId, String status) {
    }

    public record StatusResponse(
            String taskId,
            String type,
            String status,
            Integer errorCode,
            String errorMessage,
            JsonNode result
    ) {
        static StatusResponse from(AiTask task, ObjectMapper objectMapper) {
            JsonNode result = null;
            String resultJson = task.getResultJson();
            if (resultJson != null && !resultJson.isBlank()) {
                try {
                    result = objectMapper.readTree(resultJson);
                } catch (JsonProcessingException ignored) {
                    result = null;
                }
            }
            return new StatusResponse(
                    task.getId(),
                    task.getType(),
                    task.getStatus(),
                    task.getErrorCode(),
                    task.getErrorMessage(),
                    result
            );
        }
    }
}
