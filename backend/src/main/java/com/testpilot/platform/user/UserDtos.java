package com.testpilot.platform.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(max = 64, message = "用户名长度不能超过 64 个字符")
            String username,
            @NotBlank(message = "初始密码不能为空")
            @Size(min = 6, max = 128, message = "密码长度必须为 6-128 个字符")
            String password,
            @NotBlank(message = "显示名称不能为空")
            @Size(max = 64, message = "显示名称长度不能超过 64 个字符")
            String displayName,
            @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
            String email,
            @NotBlank(message = "角色不能为空")
            String role
    ) {
    }

    public record UpdateRequest(
            @NotBlank(message = "显示名称不能为空")
            @Size(max = 64, message = "显示名称长度不能超过 64 个字符")
            String displayName,
            @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
            String email,
            @NotBlank(message = "角色不能为空")
            String role
    ) {
    }

    public record StatusRequest(
            @NotBlank(message = "状态不能为空")
            String status
    ) {
    }

    public record PasswordRequest(
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 128, message = "密码长度必须为 6-128 个字符")
            String password
    ) {
    }

    public record Response(
            Long id,
            String username,
            String displayName,
            String email,
            String role,
            String status,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    user.getLastLoginAt(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
