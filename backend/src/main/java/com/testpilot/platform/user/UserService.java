package com.testpilot.platform.user;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationAuditService operationAuditService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OperationAuditService operationAuditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.operationAuditService = operationAuditService;
    }

    public List<UserDtos.Response> list(User currentUser) {
        requireAdmin(currentUser);
        return userRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(User::getCreatedAt).reversed())
                .map(UserDtos.Response::from)
                .toList();
    }

    @Transactional
    public UserDtos.Response create(UserDtos.CreateRequest request, User currentUser) {
        requireAdmin(currentUser);
        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(40903, "用户名已存在");
        }
        User user = new User(
                username,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                blankToNull(request.email()),
                normalizeRole(request.role()),
                "ENABLED"
        );
        User saved = userRepository.save(user);
        operationAuditService.record(null, currentUser, "USER_CREATE", "USER", saved.getId(),
                "创建用户 " + saved.getUsername() + "（角色 " + saved.getRole() + "）", true);
        return UserDtos.Response.from(saved);
    }

    @Transactional
    public UserDtos.Response update(Long userId, UserDtos.UpdateRequest request, User currentUser) {
        requireAdmin(currentUser);
        User user = find(userId);
        user.setDisplayName(request.displayName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setRole(normalizeRole(request.role()));
        User saved = userRepository.save(user);
        operationAuditService.record(null, currentUser, "USER_UPDATE", "USER", saved.getId(),
                "更新用户 " + saved.getUsername() + "（角色 " + saved.getRole() + "）", true);
        return UserDtos.Response.from(saved);
    }

    @Transactional
    public UserDtos.Response changeStatus(
            Long userId,
            UserDtos.StatusRequest request,
            User currentUser
    ) {
        requireAdmin(currentUser);
        User user = find(userId);
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ENABLED", "DISABLED").contains(status)) {
            throw new BusinessException(40013, "用户状态不合法");
        }
        if (user.getId().equals(currentUser.getId()) && "DISABLED".equals(status)) {
            throw new BusinessException(40014, "不能禁用当前登录用户");
        }
        user.setStatus(status);
        User saved = userRepository.save(user);
        operationAuditService.record(null, currentUser, "USER_STATUS", "USER", saved.getId(),
                "变更用户 " + saved.getUsername() + " 状态为 " + status, true);
        return UserDtos.Response.from(saved);
    }

    @Transactional
    public void resetPassword(Long userId, UserDtos.PasswordRequest request, User currentUser) {
        requireAdmin(currentUser);
        User user = find(userId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        operationAuditService.record(null, currentUser, "USER_PASSWORD", "USER", user.getId(),
                "重置用户 " + user.getUsername() + " 的密码", true);
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40404, "用户不存在"));
    }

    private void requireAdmin(User user) {
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(40303, "只有管理员可以管理用户");
        }
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ADMIN", "TEST_DEVELOPER").contains(normalized)) {
            throw new BusinessException(40015, "用户角色不合法");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
