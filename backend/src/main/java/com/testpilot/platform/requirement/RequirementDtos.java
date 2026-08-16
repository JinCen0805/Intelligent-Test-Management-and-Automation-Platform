package com.testpilot.platform.requirement;

import java.time.LocalDateTime;

public final class RequirementDtos {

    private RequirementDtos() {
    }

    public record Response(
            Long id,
            Long projectId,
            String name,
            String fileType,
            String version,
            String parseStatus,
            long contentLength,
            Long uploadedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(Requirement requirement) {
            String content = requirement.getContentText();
            return new Response(
                    requirement.getId(),
                    requirement.getProjectId(),
                    requirement.getName(),
                    requirement.getFileType(),
                    requirement.getVersion(),
                    requirement.getParseStatus(),
                    content == null ? 0 : content.length(),
                    requirement.getUploadedBy(),
                    requirement.getCreatedAt(),
                    requirement.getUpdatedAt()
            );
        }
    }

    public record DetailResponse(
            Long id,
            Long projectId,
            String name,
            String fileType,
            String version,
            String parseStatus,
            String parseError,
            String contentText,
            Long uploadedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DetailResponse from(Requirement requirement) {
            return new DetailResponse(
                    requirement.getId(),
                    requirement.getProjectId(),
                    requirement.getName(),
                    requirement.getFileType(),
                    requirement.getVersion(),
                    requirement.getParseStatus(),
                    requirement.getParseError(),
                    requirement.getContentText(),
                    requirement.getUploadedBy(),
                    requirement.getCreatedAt(),
                    requirement.getUpdatedAt()
            );
        }
    }
}
