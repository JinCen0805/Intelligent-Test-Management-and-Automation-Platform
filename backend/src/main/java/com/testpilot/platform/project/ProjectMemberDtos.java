package com.testpilot.platform.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class ProjectMemberDtos {

    private ProjectMemberDtos() {
    }

    public record UpsertRequest(
            @NotNull(message = "用户不能为空")
            Long userId,
            @NotBlank(message = "成员角色不能为空")
            String role
    ) {
    }

    public record Response(
            Long membershipId,
            Long userId,
            String username,
            String displayName,
            String email,
            String role,
            LocalDateTime joinedAt
    ) {
    }
}
