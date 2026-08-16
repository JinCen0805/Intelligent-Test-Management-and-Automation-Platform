package com.testpilot.platform.user;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<UserDtos.Response>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(userService.list(authService.currentUser(authorization)));
    }

    @PostMapping
    public ApiResponse<UserDtos.Response> create(
            @Valid @RequestBody UserDtos.CreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(userService.create(
                request,
                authService.currentUser(authorization)
        ));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserDtos.Response> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserDtos.UpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(userService.update(
                userId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/{userId}/status")
    public ApiResponse<UserDtos.Response> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserDtos.StatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(userService.changeStatus(
                userId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserDtos.PasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        userService.resetPassword(
                userId,
                request,
                authService.currentUser(authorization)
        );
        return ApiResponse.success();
    }
}
