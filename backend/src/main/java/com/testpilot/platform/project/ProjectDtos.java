package com.testpilot.platform.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "不能为空")
            @Size(max = 128, message = "长度不能超过 128 个字符")
            String name,
            @NotBlank(message = "不能为空")
            @Size(max = 64, message = "长度不能超过 64 个字符")
            String code,
            @Size(max = 2000, message = "长度不能超过 2000 个字符")
            String description
    ) {
    }

    public record UpdateRequest(
            @NotBlank(message = "不能为空")
            @Size(max = 128, message = "长度不能超过 128 个字符")
            String name,
            @Size(max = 2000, message = "长度不能超过 2000 个字符")
            String description,
            @Size(max = 16, message = "长度不能超过 16 个字符")
            String status
    ) {
    }

    public record Response(
            Long id,
            String name,
            String code,
            String description,
            Long ownerId,
            String ownerName,
            String status,
            long memberCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
