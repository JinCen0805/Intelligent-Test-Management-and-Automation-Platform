package com.testpilot.platform.auth;

import com.testpilot.platform.user.User;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "不能为空") String username,
            @NotBlank(message = "不能为空") String password
    ) {
    }

    public record UserResponse(
            Long id,
            String username,
            String displayName,
            String email,
            String role
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getRole());
        }
    }

    public record LoginResponse(String accessToken, UserResponse user) {
    }
}
