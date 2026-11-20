package com.testpilot.platform.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:user:manage')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserDtos.Response>> list(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(userService.list(user));
    }

    @PostMapping
    public ApiResponse<UserDtos.Response> create(
            @Valid @RequestBody UserDtos.CreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(userService.create(
                request,
                user
        ));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserDtos.Response> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserDtos.UpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(userService.update(
                userId,
                request,
                user
        ));
    }

    @PostMapping("/{userId}/status")
    public ApiResponse<UserDtos.Response> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserDtos.StatusRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(userService.changeStatus(
                userId,
                request,
                user
        ));
    }

    @PostMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserDtos.PasswordRequest request,
            @AuthenticationPrincipal User user
    ) {
        userService.resetPassword(
                userId,
                request,
                user
        );
        return ApiResponse.success();
    }
}
